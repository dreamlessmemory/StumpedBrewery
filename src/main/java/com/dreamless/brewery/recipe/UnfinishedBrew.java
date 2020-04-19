package com.dreamless.brewery.recipe;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.recipe.BreweryIngredient.Aspect;
import com.dreamless.brewery.recipe.BreweryIngredient.PairedAspectRarity;
import com.dreamless.brewery.recipe.BreweryIngredient.Rarity;

public class UnfinishedBrew {

	private static final double MAXIMUM_VALUE_SCALE = 1.0;
	private static final int FALLOFF_START = 10;
	private static final double MINIMUM_VALUE_SCALE = 0.25;
	private static final double FALLOFF_RATE = 0.3;

	private HashMap<Aspect, Integer> aspectMap;
	private int optimalCookTime = 0;
	private boolean distilled = false;

	public UnfinishedBrew(Inventory inventory, int cookTime) {
		
		// Add to temp map
		HashMap<PairedAspectRarity, Integer> tempMap = new HashMap<PairedAspectRarity, Integer>();
		for(ItemStack itemStack : inventory.getContents())
		{
			if(itemStack == null) {
				continue;
			}
			Aspect aspect = Aspect.getAspect(itemStack.getType());
			Rarity rarity = Rarity.getRarity(itemStack.getType());
			PairedAspectRarity pairedAspectRarity = new PairedAspectRarity(aspect, rarity);
			tempMap.put(pairedAspectRarity, tempMap.getOrDefault(pairedAspectRarity, 0) + rarity.getValue());
			optimalCookTime += rarity.getCookTime();
		}
		
		// Set up final map
		aspectMap = new HashMap<Aspect, Integer>();
		for(Entry<PairedAspectRarity, Integer> entry : tempMap.entrySet()) {
			aspectMap.put(entry.getKey().aspect, aspectMap.getOrDefault(entry.getKey().aspect, 0) +
					Math.max(entry.getKey().rarity.getSaturation(), entry.getValue()));
		}
		
		int deviation = Math.abs(optimalCookTime - cookTime);
		final double scalar = (deviation < FALLOFF_START) ? 
				MAXIMUM_VALUE_SCALE :  Math.max(MAXIMUM_VALUE_SCALE - (deviation * FALLOFF_RATE), MINIMUM_VALUE_SCALE);

		// Update Map
		aspectMap.forEach((aspect, value) -> aspectMap.replace(aspect, (int)(value * scalar)));  
	}

	public final HashMap<Aspect, Integer> getAspectMap() {
		return aspectMap;
	}

	public final boolean isDistilled() {
		return distilled;
	}

	public final void setDistilled(boolean distilled) {
		this.distilled = distilled;
	}
}
