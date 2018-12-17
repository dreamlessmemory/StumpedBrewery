package com.dreamless.brewery;

import org.bukkit.Material;
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
	
	private ArrayList<ItemStack> ingredients = new ArrayList<ItemStack>();
	private HashMap<String, Aspect> aspects = new HashMap<String, Aspect>();
	private int cookedTime;
	private String type;

	// Represents ingredients in Cauldron, Brew
	// Init a new BIngredients
	public BIngredients() {
	}

	// Load from File
	public BIngredients(ArrayList<ItemStack> ingredients, HashMap<String, Aspect> aspects, int cookedTime, String type) {
		this.ingredients = ingredients;
		this.aspects = aspects;
		this.cookedTime = cookedTime;
		this.type = type;
	}

	// Add an ingredient to this
	public void add(ItemStack ingredient) {
		//SQL
		String aspectQuery = "name, aspect1name, aspect1rating, aspect2name, aspect2rating, aspect3name, aspect3rating";
		String query = "SELECT " + aspectQuery + " FROM ingredients WHERE name=?";
		
		//Add Item
		int ingPosition = getIndexOf(ingredient);
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
					double newPotency = Aspect.calculateRarityPotency(results.getInt("aspect1rating"));
					double newSaturation = Aspect.calculateRaritySaturation(results.getInt("aspect1rating"));
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
					double newPotency = Aspect.calculateRarityPotency(results.getInt("aspect2rating"));
					double newSaturation = Aspect.calculateRaritySaturation(results.getInt("aspect2rating"));
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
					double newPotency = Aspect.calculateRarityPotency(results.getInt("aspect3rating"));
					double newSaturation = Aspect.calculateRaritySaturation(results.getInt("aspect3rating"));
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
		
		/*if(Brewery.debug) {
			Brewery.breweryDriver.debugLog("Added: " + ingredient.toString());
			Set<String> keys = aspects.keySet();
			for (String key : keys) {
				Brewery.breweryDriver.debugLog(aspects.get(key).toString());
			}
		}*/
	}


	// returns an Potion item with cooked ingredients
	public ItemStack cook(int state, Player player) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// cookedTime is always time in minutes, state may differ with number of ticks
		cookedTime = state;
		/** Aspect Calculation **/
		//Split Aspects
		HashMap<String, Aspect> effectAspects = new HashMap<String, Aspect>();
		HashMap<String, Aspect> flavorAspects = new HashMap<String, Aspect>();
		
		for(String currentAspect: aspects.keySet()) {
			if(currentAspect.contains("_DURATION") || currentAspect.contains("_POTENCY")) effectAspects.put(currentAspect, aspects.get(currentAspect));
			else flavorAspects.put(currentAspect, aspects.get(currentAspect));
		}
		
		TreeMap <String, Double> effectAspectActivation = new TreeMap<String, Double>();
		TreeMap <String, Double> flavorAspectActivation = new TreeMap<String, Double>();
		TreeMap <String, Double> effectAspectEffectivePotency = new TreeMap<String, Double>();
		TreeMap <String, Double> flavorAspectEffectivePotency = new TreeMap<String, Double>();
		
		//Calculate Effect Values
		//Add calculated aspects to the map
		for(String currentAspect: effectAspects.keySet()) {
			Aspect aspect = effectAspects.get(currentAspect);
			double calculatedPotency = aspect.getPotency();
			double calculatedSaturation = (aspect.getSaturation() < 1 ? 1 : aspect.getSaturation());
			double effectiveActivation = Aspect.getEffectiveActivation(currentAspect, aspect.getActivation(), type);
			effectAspectActivation.put(currentAspect, effectiveActivation);
			effectAspectEffectivePotency.put(currentAspect, calculatedPotency / calculatedSaturation * effectiveActivation);
			Brewery.breweryDriver.debugLog("PUT " + currentAspect + " " + effectAspectActivation.get(currentAspect));
		}
		
		//Remove until 3 effects
		while(effectAspectEffectivePotency.size() > 3) {
			Map.Entry<String, Double> entry = effectAspectEffectivePotency.pollFirstEntry();
			effectAspectActivation.remove(entry.getKey());
			effectAspects.remove(entry.getKey());
			Brewery.breweryDriver.debugLog("KNOCKOUT " + entry.getKey());
		}
		
		//Calculate Flavor Values
		//Add calculated aspects to the map
		for(String currentAspect: flavorAspects.keySet()) {
			Aspect aspect = flavorAspects.get(currentAspect);
			double calculatedPotency = aspect.getPotency();
			double calculatedSaturation = (aspect.getSaturation() < 1 ? 1 : aspect.getSaturation());
			double effectiveActivation = Aspect.getEffectiveActivation(currentAspect, aspect.getActivation(), type);
			flavorAspectActivation.put(currentAspect, effectiveActivation);
			flavorAspectEffectivePotency.put(currentAspect, calculatedPotency /calculatedSaturation * effectiveActivation);
			Brewery.breweryDriver.debugLog("PUT " + currentAspect + " " + flavorAspectActivation.get(currentAspect));
		}
		//Brewery.breweryDriver.debugLog("SIZE? " + effectAspectValues.size());
		
		//Remove until 6 flavors
		while(flavorAspectEffectivePotency.size() > 6) {
			Map.Entry<String, Double> entry = flavorAspectEffectivePotency.pollFirstEntry();
			flavorAspectActivation.remove(entry.getKey());
			flavorAspects.remove(entry.getKey());
			Brewery.breweryDriver.debugLog("KNOCKOUT " + entry.getKey());
		}
		
		
		//Add custom potion effects based on effect aspects
		ArrayList<PotionEffect> effects = BEffect.calculateEffect(new HashMap<String, Double>(effectAspectEffectivePotency));
		for (PotionEffect effect: effects) {
			potionMeta.addCustomEffect(effect, true);
		}
		
		
		//Recipe
		BRecipe recipe = BRecipe.getRecipe(player, type, effectAspectEffectivePotency, flavorAspectEffectivePotency, false, false);
		potionMeta.setDisplayName(recipe.getName());
		ArrayList<String> craftersList = new ArrayList<String>();
		craftersList.add(player.getDisplayName());
		potionMeta.setLore(recipe.getFlavorText(craftersList));
		potionMeta.setColor(BRecipe.getColor(type));

		potion.setItemMeta(potionMeta);
		
		//Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound("brewery"); //All brewery NBT gets set here.
		//Write NBT data
		//Aspect Base Potency
		NBTCompound aspectTagList = breweryMeta.addCompound("aspectsBase");
		for(String currentAspect: effectAspects.keySet()) {
			Aspect aspect = effectAspects.get(currentAspect);
			aspectTagList.setDouble(currentAspect, aspect.getPotency()/aspect.getSaturation());
		}
		for(String currentAspect: flavorAspects.keySet()) {
			Aspect aspect = flavorAspects.get(currentAspect);
			aspectTagList.setDouble(currentAspect, aspect.getPotency()/aspect.getSaturation());
		}
		
		//Aspect Activation
		NBTCompound aspectActList = breweryMeta.addCompound("aspectsActivation");
		for(String currentAspect: effectAspectActivation.keySet()) {
			aspectActList.setDouble(currentAspect, effectAspectActivation.get(currentAspect));
		}
		for(String currentAspect: flavorAspectActivation.keySet()) {
			aspectActList.setDouble(currentAspect, flavorAspectActivation.get(currentAspect));
		}
		//Multipliers
		breweryMeta.setDouble("potency", 1.0);
		breweryMeta.setDouble("duration", 1.0);
		
		
		//Type
		breweryMeta.setString("type", type);
		//Crafters
		NBTCompound crafters = breweryMeta.addCompound("crafters");
		crafters.setString(player.getDisplayName(), player.getDisplayName());
		potion = nbti.getItem();

		return potion;
	}

	public void fermentOneStep(int state) {
		for(String currentAspect : aspects.keySet()) {
			Aspect aspect = aspects.get(currentAspect);
			
			double activationIncrease = Aspect.getFermentationIncrease(state, currentAspect, type);
			double newActivation = aspect.getActivation()+ activationIncrease;
			Brewery.breweryDriver.debugLog("Update Activation of " + currentAspect + ": " + aspect.getActivation() + " + " + activationIncrease + " -> " + newActivation);
			aspect.setActivation(newActivation);
			aspects.put(currentAspect, aspect);
		}
	}

	public int getCookedTime() {
		return cookedTime;
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
			type = "ELIXIR";
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

	public HashMap<String, Aspect> getAspects() {
		return aspects;
	}

	public void setAspects(HashMap<String, Aspect> aspects) {
		this.aspects = aspects;
	}

	public ArrayList<ItemStack> getIngredients() {
		return ingredients;
	}

	public void setIngredients(ArrayList<ItemStack> ingredients) {
		this.ingredients = ingredients;
	}
	
	private int getIndexOf(ItemStack item) {
		for(ItemStack i: ingredients) {
			if(item.isSimilar(i)) {
				return ingredients.indexOf(i);
			}
		}
		return -1;
	}
}