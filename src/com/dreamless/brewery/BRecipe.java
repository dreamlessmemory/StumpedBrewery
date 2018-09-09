package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BRecipe {
	
	//Difficulty Adjustment
	public static float ingredientDifficultyScale = 11.0f;
	public static float fermentationDifficultyScale = 11.0f;
	public static float woodTypeDifficultyScale = 1.0f;

	private String name;
	private ArrayList<ItemStack> ingredients = new ArrayList<ItemStack>(); // material and amount
	private int cookingTime; // time to cook in cauldron
	private int distillruns; // runs through the brewer
	private int distillTime; // time for one distill run in seconds
	private byte wood; // type of wood the barrel has to consist of
	private int age; // time in minecraft days for the potions to age in barrels
	private String color; // color of the destilled/finished potion
	private int difficulty; // difficulty to brew the potion, how exact the instruction has to be followed
	private int alcohol; // Alcohol in perfect potion
	private ArrayList<BEffect> effects = new ArrayList<BEffect>(); // Special Effects when drinking
    private String flavorText;
    
    
    private Map<String, Integer> aspects = new HashMap<String, Integer>();

	public BRecipe(ConfigurationSection configSectionRecipes, String recipeId) {
        //parse name
		name = configSectionRecipes.getString(recipeId + ".name");
		
        //parse ingredients
		ConfigurationSection aspectsSection = configSectionRecipes.getConfigurationSection(recipeId+ ".aspects");
		for (String aspect: aspectsSection.getKeys(false)) {
			aspects.put(aspect, aspectsSection.getInt(aspect));
			P.p.debugLog(aspect + " - " + aspectsSection.getInt(aspect));
		}
        //parse flavorText
		flavorText = configSectionRecipes.getString(recipeId + ".flavortext");
        //parse the rest
		this.color = configSectionRecipes.getString(recipeId + ".color", "AQUA");
		this.alcohol = configSectionRecipes.getInt(recipeId+ ".alcohol", 0);

	}

	// check every part of the recipe for validity
	public boolean isValid() {
		return !(name == null || aspects.isEmpty());
	}

	// allowed deviation to the recipes count of ingredients at the given difficulty
	public int allowedCountDiff(int count) {
		if (count < 8) {
			count = 8;
		}
		int allowedCountDiff = Math.round((float) ((ingredientDifficultyScale - difficulty) * (count / 10.0)));

		if (allowedCountDiff == 0) {
			return 1;
		}
		return allowedCountDiff;
	}

	// allowed deviation to the recipes cooking-time at the given difficulty
	public int allowedTimeDiff(int time) {
		if (time < 8) {
			time = 8;
		}
		int allowedTimeDiff = Math.round((float) ((fermentationDifficultyScale - difficulty) * (time / 10.0)));

		if (allowedTimeDiff == 0) {
			return 1;
		}
		return allowedTimeDiff;
	}

	// difference between given and recipe-wanted woodtype
	public float getWoodDiff(float wood) {
		return Math.abs(wood - this.wood) * woodTypeDifficultyScale;
	}

	public boolean isCookingOnly() {
		return age == 0 && distillruns == 0;
	}

	public boolean needsDistilling() {
		return distillruns != 0;
	}

	public boolean needsToAge() {
		return age != 0;
	}

	public boolean hasFlavorText(){
        return flavorText == null;
    }
    
    // true if given list misses an ingredient
	public boolean isMissingIngredients(List<ItemStack> list) {
		if (list.size() < ingredients.size()) {
			return true;
		}
		for (ItemStack ingredient : ingredients) {
			boolean matches = false;
			for (ItemStack used : list) {
				if (ingredientsMatch(used, ingredient)) {
					matches = true;
					break;
				}
			}
			if (!matches) {
				return true;
			}
		}
		return false;
	}
	
	public int countMissingIngredients(List<ItemStack> list) {
		int count = 0;
		//Code
		for (ItemStack ingredient : ingredients) {
			boolean matches = false;
			for (ItemStack used : list) {
				if (ingredientsMatch(used, ingredient)) {
					matches = true;
					break;
				}
			}
			if (!matches) {
				count++;
			}
		}
		return count;
	}

	// Returns true if this ingredient cares about durability
	public boolean hasExactData(ItemStack item) {
		for (ItemStack ingredient : ingredients) {
			if (ingredient.getType().equals(item.getType())) {
				return ingredient.getDurability() != -1;
			}
		}
		return true;
	}

	// Returns true if this item matches the item from a recipe
	public static boolean ingredientsMatch(ItemStack usedItem, ItemStack recipeItem) {
		if (!recipeItem.getType().equals(usedItem.getType())) {
			return false;
		}
		return recipeItem.getDurability() == -1 || recipeItem.getDurability() == usedItem.getDurability();
	}

	// Create a Potion from this Recipe with best values. Quality can be set, but will reset to 10 if put in a barrel
	public ItemStack create(int quality) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		int uid = Brew.generateUID();

		ArrayList<ItemStack> list = new ArrayList<ItemStack>(ingredients.size());
		for (ItemStack item : ingredients) {
			if (item.getDurability() == -1) {
				list.add(new ItemStack(item.getType(), item.getAmount()));
			} else {
				list.add(item.clone());
			}
		}

		BIngredients bIngredients = new BIngredients(list, cookingTime);

		Brew brew = new Brew(uid, bIngredients, quality, distillruns, getAge(), wood, getName(), false, false, true, 0);

		Brew.PotionColor.valueOf(getColor()).colorBrew(potionMeta, potion, false);
		potionMeta.setDisplayName(P.p.color("&f" + getName()));
		// This effect stores the UID in its Duration
		potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);

		brew.convertLore(potionMeta, false);
		Brew.addOrReplaceEffects(potionMeta, effects, quality);
		brew.touch();
		
		potion.setItemMeta(potionMeta);
		return potion;
	}


	// Getter

	// how many of a specific ingredient in the recipe
	public int amountOf(ItemStack item) {
		for (ItemStack ingredient : ingredients) {
			if (ingredientsMatch(item, ingredient)) {
				return ingredient.getAmount();
			}
		}
		return 0;
	}

	// name that fits the quality
	public String getName() {
		return name;
	}

    public String getFlavorText(int quality) {
    	return flavorText;
	}
    
	// If one of the quality names equalIgnoreCase given name
	public boolean hasName(String name) {
		return this.name.equalsIgnoreCase(name);
	}

	public int getCookingTime() {
		return cookingTime;
	}

	public int getDistillRuns() {
		return distillruns;
	}

	public int getDistillTime() {
		return distillTime;
	}

	public String getColor() {
		if (color != null) {
			return color.toUpperCase();
		}
		return "BLUE";
	}

	// get the woodtype
	public byte getWood() {
		return wood;
	}

	public float getAge() {
		return (float) age;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public int getAlcohol() {
		return alcohol;
	}

	public ArrayList<BEffect> getEffects() {
		return effects;
	}

}