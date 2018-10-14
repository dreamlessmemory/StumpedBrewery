package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BIngredients {
	private static int lastId = 0;

	private int id;
	
	private ArrayList<ItemStack> ingredients = new ArrayList<ItemStack>();
	private HashMap<String, Aspect> aspects = new HashMap<String, Aspect>();
	private int cookedTime;
	private String type;

	// Represents ingredients in Cauldron, Brew
	// Init a new BIngredients
	public BIngredients() {
		this.id = lastId;
		lastId++;
	}

	// Load from File
	public BIngredients(ArrayList<ItemStack> ingredients, int cookedTime) {
		this.ingredients = ingredients;
		this.cookedTime = cookedTime;
		this.id = lastId;
		lastId++;
	}

	// Add an ingredient to this
	public void add(ItemStack ingredient) {
		//SQL
		String aspectQuery = "name, aspect1name, aspect1rating, aspect2name, aspect2rating, aspect3name, aspect3rating";
		String query = "SELECT " + aspectQuery + " FROM ingredients WHERE name=?";
		
		//Add Item
		int ingPosition = ingredients.indexOf(ingredient);
		if(ingPosition != -1) {
			ingredients.get(ingPosition).setAmount(ingredients.get(ingPosition).getAmount() + ingredient.getAmount());
		} else {
			ingredients.add(ingredient);
		}
		//Aspect multipliers
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, ingredient.getType().name());
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("Failed to poll SQL");
			} else {//Successful Pull
				String aspect1 = results.getString("aspect1name");
				String aspect2 = results.getString("aspect2name");
				String aspect3 = results.getString("aspect3name");
				//Aspect 1
				if(aspect1 != null) {
					Aspect aspect = aspects.get(aspect1);
					double newPotency = Aspect.calculateRarityPotency(results.getInt("aspect1rating")) * ingredient.getAmount();
					double newSaturation = Aspect.calculateRaritySaturation(results.getInt("aspect1rating")) * ingredient.getAmount();
					if (aspect != null) {//aspect is found
						aspect.setPotency(newPotency + aspect.getPotency());
						aspect.setSaturation(newSaturation + aspect.getSaturation());
					} else {
						Aspect newAspect = new Aspect (newPotency, newSaturation);
						aspects.put(aspect1, newAspect);
					}
				}
				//Aspect 2
				if(aspect2 != null) {
					Aspect aspect = aspects.get(aspect2);
					double newPotency = Aspect.calculateRarityPotency(results.getInt("aspect2rating")) * ingredient.getAmount();
					double newSaturation = Aspect.calculateRaritySaturation(results.getInt("aspect2rating")) * ingredient.getAmount();
					if (aspect != null) {//aspect is found
						aspect.setPotency(newPotency + aspect.getPotency());
						aspect.setSaturation(newSaturation + aspect.getSaturation());
					} else {
						Aspect newAspect = new Aspect (newPotency, newSaturation);
						aspects.put(aspect2, newAspect);
					}
				}
				//Aspect 3
				if(aspect3 != null) {
					Aspect aspect = aspects.get(aspect3);
					double newPotency = Aspect.calculateRarityPotency(results.getInt("aspect3rating")) * ingredient.getAmount();
					double newSaturation = Aspect.calculateRaritySaturation(results.getInt("aspect3rating")) * ingredient.getAmount();
					if (aspect != null) {//aspect is found
						aspect.setPotency(newPotency + aspect.getPotency());
						aspect.setSaturation(newSaturation + aspect.getSaturation());
					} else {
						Aspect newAspect = new Aspect (newPotency, newSaturation);
						aspects.put(aspect3, newAspect);
					}
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		if(Brewery.debug) {
			Brewery.breweryDriver.debugLog("Added: " + ingredient.toString());
			Set<String> keys = aspects.keySet();
			for (String key : keys) {
				Brewery.breweryDriver.debugLog(aspects.get(key).toString());
			}
		}
	}


	// returns an Potion item with cooked ingredients
	public ItemStack cook(int state, Player player) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// cookedTime is always time in minutes, state may differ with number of ticks
		cookedTime = state;
		/** Aspect Calculation **/
		HashMap <String, Double> cookedAspects = new HashMap<String, Double>();
		//Determine overflow
		double aspectSaturation = (aspects.size() <= 6 ? 1 : aspects.size() / 6);
		//Add calculated aspects to the map
		for(String currentAspect: aspects.keySet()) {
			Aspect aspect = aspects.get(currentAspect);
			double calculatedPotency = aspect.getPotency();
			double calculatedSaturation = (aspect.getSaturation() < 1 ? 1 : aspect.getSaturation());
			cookedAspects.put(currentAspect, (calculatedPotency / (calculatedSaturation * aspectSaturation)));
			Brewery.breweryDriver.debugLog("PUT " + currentAspect + " " + cookedAspects.get(currentAspect));
		}
		Brewery.breweryDriver.debugLog("SIZE? " + cookedAspects.size());
		//Add effects based on aspects
		ArrayList<PotionEffect> effects = BEffect.calculateEffect(new HashMap<String, Double>(cookedAspects));
		for (PotionEffect effect: effects) {
			potionMeta.addCustomEffect(effect, true);
		}
		
		
		//Recipe
		BRecipe recipe = BRecipe.getRecipe(player, type, cookedAspects, false, false);
		potionMeta.setDisplayName(recipe.getName());
		potionMeta.setLore(recipe.getFlavorText());
		potionMeta.setColor(BRecipe.getColor(type));

		potion.setItemMeta(potionMeta);
		
		//Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound("brewery"); //All brewery NBT gets set here.
		//Write NBT data
		for(String currentAspect: cookedAspects.keySet()) {
			breweryMeta.setDouble(currentAspect, cookedAspects.get(currentAspect));
		}
		breweryMeta.setString("type", type);
		potion = nbti.getItem();

		return potion;
	}

	public void fermentOneStep(int state) {
		for(String currentAspect : aspects.keySet()) {
			Aspect aspect = aspects.get(currentAspect);
			double fermentationBonus = Aspect.getStepBonus(state, currentAspect, "fermentation");
			double typeBonus = fermentationBonus * (Aspect.getStepBonus(state, type, "fermentation")/100);
			double newPotency = aspect.getPotency() + fermentationBonus + typeBonus;
			if(newPotency <= 0) {
				newPotency = 0;
			}
			Brewery.breweryDriver.debugLog("Update Potency of " + currentAspect + ": " + aspect.getPotency() + " + " + fermentationBonus + " + " + typeBonus + " -> " + newPotency);
			aspect.setPotency(newPotency);
			aspects.put(currentAspect, aspect);
		}
	}

	public int getCookedTime() {
		return cookedTime;
	}

	// saves data into main Ingredient section. Returns the save id
	public int save(ConfigurationSection config) {
		String path = "Ingredients." + id;
		if (cookedTime != 0) {
			config.set(path + ".cookedTime", cookedTime);
		}
		config.set(path + ".mats", serializeIngredients());
		return id;
	}

	//convert the ingredient Material to String
	public Map<String, Integer> serializeIngredients() {
		Map<String, Integer> mats = new HashMap<String, Integer>();
		for (ItemStack item : ingredients) {
			String mat = item.getType().name() + "," + item.getDurability();
			mats.put(mat, item.getAmount());
		}
		return mats;
	}
	
	public String getContents() {
		String manifest = " ";
		for (ItemStack item: ingredients) {
			String itemName = "";
			for(String part: item.getType().toString().split("_")) {
				itemName = itemName.concat(part.substring(0, 1) + part.substring(1).toLowerCase()+  " ");
			}
			manifest = manifest.concat(itemName + "x" + item.getAmount() + " - ");
		}
		return manifest.substring(0, manifest.length() - 3);
	}
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void calculateType(){
		//SQL
		String query = "SELECT * FROM brewtypes WHERE material=?";
		int highestCount = 0;
		int priority = 0;
		for (ItemStack ingredient : ingredients) {
			//Pull from DB
			try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
				stmt.setString(1, ingredient.getType().name());
				ResultSet results;
				results = stmt.executeQuery();
				if (results.next()) {
					if(ingredient.getAmount() > highestCount && results.getInt("priority") > priority) {
						highestCount = ingredient.getAmount();
						priority = results.getInt("priority");
						type = results.getString("type");
					}
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		if(highestCount == 0) {//no one is the right one
			type = "OTHER";
		}
		Brewery.breweryDriver.debugLog("Starting brew: " + type);
	}
	
	public void startCooking() {
		calculateType();
	}
	public static boolean acceptableIngredient(Material material){
		//SQL
		String query = "SELECT EXISTS(SELECT 1 FROM ingredients WHERE name='" + material.name() + "')";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				return false;
			} else {
				return results.getInt(1) == 1;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return false;
	}
}