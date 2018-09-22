package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BIngredients {
	public static Set<Material> acceptableIngredients = new HashSet<Material>();
	public static Hashtable<Material, Ingredient> ingredientInfo = new Hashtable<Material, Ingredient>();
	public static HashMap<String, String> typeMap = new HashMap<String, String>();
	
	private static int lastId = 0;
	
	
	
	public static float ageDifficultyScale = 2.0f;
	public static float ingredientScoreMultiplier = 2.0f;
	public static float cookingScoreMultiplier = 1.0f;
	public static float woodTypeScoreMultiplier = 1.0f;
	public static float ageScoreMultiplier = 2.0f;

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

		/*for (ItemStack item : ingredients) {
			addMaterial(item);
		}*/
	}

	// Add an ingredient to this
	public void add(ItemStack ingredient) {
		//Add Item
		int ingPosition = ingredients.indexOf(ingredient);
		if(ingPosition != -1) {
			ingredients.get(ingPosition).setAmount(ingredients.get(ingPosition).getAmount() + ingredient.getAmount());
		} else {
			ingredients.add(ingredient);
		}
		
		//Ingredient Info
		Ingredient info = ingredientInfo.get(ingredient.getType());
		//Multipliers
		
		String aspectQuery = "name, aspect1name, aspect1rating, aspect2name, aspect2rating, aspect3name, aspect3rating";
		
		//SQL test
		try {
			String query = "SELECT " + aspectQuery + " FROM ingredients WHERE name='" + info.getType().name() + "'";
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("Failed to poll SQL");
			} else {//Successful Pull
				//Aspect 1
				if(results.getString("aspect1name") != null) {
					Aspect aspect = aspects.get(results.getString("aspect1name"));
					if (aspect != null) {//aspect is found
						aspect.setPotency(aspect.getPotency() + (BIngredients.calculateRarityPotency(results.getInt("aspect1rating")) * ingredient.getAmount()));
						aspect.setSaturation(aspect.getSaturation() + (BIngredients.calculateRaritySaturation(results.getInt("aspect1rating")) * ingredient.getAmount()));
					} else {
						Aspect nAspect = new Aspect (results.getString("aspect1name"), BIngredients.calculateRarityPotency(results.getInt("aspect1rating")) * ingredient.getAmount(), BIngredients.calculateRaritySaturation(results.getInt("aspect1rating")) * ingredient.getAmount());
						aspects.put(results.getString("aspect1name"), nAspect);
					}
				}
				//Aspect 2
				if(results.getString("aspect2name") != null) {
					Aspect aspect = aspects.get(results.getString("aspect2name"));
					if (aspect != null) {//aspect is found
						aspect.setPotency(aspect.getPotency() + (BIngredients.calculateRarityPotency(results.getInt("aspect2rating")) * ingredient.getAmount()));
						aspect.setSaturation(aspect.getSaturation() + (BIngredients.calculateRaritySaturation(results.getInt("aspect2rating")) * ingredient.getAmount()));
					} else {
						Aspect nAspect = new Aspect (results.getString("aspect2name"), BIngredients.calculateRarityPotency(results.getInt("aspect2rating")) * ingredient.getAmount(), BIngredients.calculateRaritySaturation(results.getInt("aspect2rating")) * ingredient.getAmount());
						aspects.put(results.getString("aspect2name"), nAspect);
					}
				}
				//Aspect 3
				if(results.getString("aspect3name") != null) {
					Aspect aspect = aspects.get(results.getString("aspect3name"));
					if (aspect != null) {//aspect is found
						aspect.setPotency(aspect.getPotency() + (BIngredients.calculateRarityPotency(results.getInt("aspect3rating")) * ingredient.getAmount()));
						aspect.setSaturation(aspect.getSaturation() + (BIngredients.calculateRaritySaturation(results.getInt("aspect3rating")) * ingredient.getAmount()));
					} else {
						Aspect nAspect = new Aspect (results.getString("aspect3name"), BIngredients.calculateRarityPotency(results.getInt("aspect3rating")) * ingredient.getAmount(), BIngredients.calculateRaritySaturation(results.getInt("aspect3rating")) * ingredient.getAmount());
						aspects.put(results.getString("aspect3name"), nAspect);
					}
				}
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
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
	public ItemStack cook(int state) {//TODO: things

		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// cookedTime is always time in minutes, state may differ with number of ticks
		cookedTime = state;

		//Test Custom NBT
		NBTItem nbti = new NBTItem(potion);
		//All brewery NBT gets set here.
		NBTCompound breweryMeta = nbti.addCompound("brewery");
		breweryMeta.setString("test", "BLAH");
		
		//Then add your effects?
		//potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.GLOWING, 2000, 1), true);
		
		potion = nbti.getItem();
		//potion.setItemMeta(potionMeta);
		
		return potion;
	}

	public void fermentOneStep(int state) {
		for(String containedAspects : aspects.keySet()) {
			Aspect aspect = aspects.get(containedAspects);
			double fermentationBonus = Aspect.aspectStageMultipliers.get(containedAspects).getFermentationStageStep(state);
			double typeBonus = fermentationBonus * (Aspect.aspectStageMultipliers.get(type).getFermentationMultiplier() - 1);
			double newPotency = aspect.getPotency() + fermentationBonus + typeBonus;
			if(newPotency <= 0) {
				newPotency = 0;
			}
			aspect.setPotency(newPotency);
			Brewery.breweryDriver.debugLog("Update Potency of " + containedAspects + " - " + newPotency);
			aspects.put(containedAspects, aspect);
		}
	}

	/*public Map<Material, Integer> getIngredients() {
		return ingredients;
	}*/

	public int getCookedTime() {
		return cookedTime;
	}
	
	// Creates a copy ingredients
	public BIngredients clone() {
		BIngredients copy = new BIngredients();
		copy.ingredients.addAll(ingredients);
		//copy.materials.putAll(materials);
		copy.cookedTime = cookedTime;
		return copy;
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
			///System.out.println(item.getType().toString() + ": " + item.getAmount());
			String itemName = "";
			for(String part: item.getType().toString().split("_")) {
				itemName = itemName.concat(part.substring(0, 1) + part.substring(1).toLowerCase()+  " ");
			}
			manifest = manifest.concat(itemName + "x" + item.getAmount() + " - ");
		}
		return manifest.substring(0, manifest.length() - 3);
	}

	public double calcuateFermentationMultiplier() {
		double multiplier = 0.0;
		int counter = 0;
		for(ItemStack item: ingredients) {
			Ingredient info = ingredientInfo.get(item.getType());
			multiplier += info.getFermentationMultiplier() * item.getAmount();
			counter += item.getAmount();
		}
		return multiplier/counter;
	}
	
	public double calcuateAgingMultiplier() {
		double multiplier = 0.0;
		int counter = 0;
		for(ItemStack item: ingredients) {
			Ingredient info = ingredientInfo.get(item.getType());
			multiplier += info.getAgingMultiplier() * item.getAmount();
			counter += item.getAmount();
		}
		return multiplier/counter;
	}
	
	public double calcuateDistillingMultiplier() {
		double multiplier = 0.0;
		int counter = 0;
		for(ItemStack item: ingredients) {
			Ingredient info = ingredientInfo.get(item.getType());
			multiplier += info.getDistillingMultiplier() * item.getAmount();
			counter += item.getAmount();
		}
		return multiplier/counter;
	}
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void calculateType() {
		Material highest = null;
		int highestCount = 0;
		for (ItemStack ingredient : ingredients) {
			if (typeMap.containsKey(ingredient.getType().name())) {
				if(ingredient.getAmount() > highestCount) {
					highest = ingredient.getType();
					highestCount = ingredient.getAmount();
				}
			}
		}
		//Assign a Name if found
		if (highest != null) {
			type = typeMap.get(highest.name());
			Brewery.breweryDriver.debugLog("BREW IS A " + type);
		}
		
	}
	
	public void startCooking() {
		calculateType();
	}
	
	public static double calculateRarityPotency(int rarity){
		switch(rarity) {
			case (1):
				return 6;
			case (2):
				return 20;
			case (3):
				return 42;
			case (4):
				return 64;
			case (5):
				return 100;
			default:
				return 6;
		}
	}
	
	public static double calculateRaritySaturation(int rarity){
		switch(rarity) {
		case (1):
			return 0.2;
		case (2):
			return 0.4;
		case (3):
			return 0.6;
		case (4):
			return 0.8;
		case (5):
			return 1.0;
		default:
			return 0.2;
	}
	}
}