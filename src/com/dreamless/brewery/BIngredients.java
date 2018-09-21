package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;

import java.util.*;

public class BIngredients {
	public static Set<Material> acceptableIngredients = new HashSet<Material>();
	public static Hashtable<Material, Ingredient> ingredientInfo = new Hashtable<Material, Ingredient>();
	public static HashMap<String, String> typeMap = new HashMap<String, String>();
	public static ArrayList<BRecipe> recipes = new ArrayList<BRecipe>();
	public static Map<Material, String> cookedNames = new HashMap<Material, String>();
	public static ArrayList<String> baseIngredients = new ArrayList<String>();
	
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
				
				
		//Aspects
		Map<String, String> ingAspects = info.getAspects();//Get the aspects from the ingredient
		for (String aspect : ingAspects.keySet()) {//work on each aspect
			Aspect cAspect = aspects.get(aspect);
			if (cAspect != null) {//aspect is found
				cAspect.setPotency(cAspect.getPotency() + (Aspect.calculateRarityPotency(ingAspects.get(aspect)) * ingredient.getAmount()));
				cAspect.setSaturation(cAspect.getSaturation() + (Aspect.calculateRaritySaturation(ingAspects.get(aspect)) * ingredient.getAmount()));
			} else {
				Aspect nAspect = new Aspect (aspect, Aspect.calculateRarityPotency(ingAspects.get(aspect)) * ingredient.getAmount(), Aspect.calculateRaritySaturation(ingAspects.get(aspect)) * ingredient.getAmount());
				aspects.put(aspect, nAspect);
			}
		}
		if(P.debug) {
			P.p.debugLog("Added: " + ingredient.toString());
			Set<String> keys = aspects.keySet();
			for (String key : keys) {
				P.p.debugLog(aspects.get(key).toString());
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
			double typeBonus = fermentationBonus * (1 - Aspect.aspectStageMultipliers.get(type).getFermentationMultiplier());
			double newPotency = aspect.getPotency() + fermentationBonus + typeBonus;
			if(newPotency <= 0) {
				newPotency = 0;
			}
			aspect.setPotency(newPotency);
			P.p.debugLog("Update Potency of " + containedAspects + " - " + newPotency);
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
			P.p.debugLog("BREW IS A " + type);
		}
		
	}
	
	public void startCooking() {
		calculateType();
	}
}