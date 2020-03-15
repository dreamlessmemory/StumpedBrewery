package com.dreamless.brewery.recipe;

import java.util.HashMap;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.recipe.BreweryIngredient.Aspect;
import com.dreamless.brewery.recipe.BreweryIngredient.Rarity;

public class UnfinishedBrew {
	
	private static final double MAXIMUM_VALUE_SCALE = 1.0;
	private static final int FALLOFF_START = 10;
	private static final double MINIMUM_VALUE_SCALE = 0.25;
	private static final double FALLOFF_RATE = 0.3;

	private HashMap<Aspect, Integer> aspectMap;
	private int optimalCookTime = 0;
	private boolean fermented = false;
	private boolean distilled = false;
	
	public UnfinishedBrew(Inventory inventory) {
		for(ItemStack itemStack : inventory.getContents())
		{
			Aspect aspect = Aspect.getAspect(itemStack.getType());
			if(aspect != Aspect.INVALID) {
				Rarity rarity = Rarity.getRarity(itemStack.getType());
				aspectMap.put(aspect, aspectMap.getOrDefault(aspect, 0) + rarity.getValue());
				optimalCookTime += rarity.getCookTime();
			}
		}
	}
	
	public void finishFermentation(int cookTime) {
		if (!fermented) {
			// Scale effectiveness
			int deviation = Math.abs(optimalCookTime - cookTime);
			final double scalar = (deviation < FALLOFF_START) ? 
					MAXIMUM_VALUE_SCALE :  Math.max(MAXIMUM_VALUE_SCALE - (deviation * FALLOFF_RATE), MINIMUM_VALUE_SCALE);
			
			// Update Map
			aspectMap.forEach((aspect, value) -> aspectMap.replace(aspect, (int)(value * scalar)));  
			fermented = true;
		}
	}

	public final HashMap<Aspect, Integer> getAspectMap() {
		return aspectMap;
	}
	
	public final boolean isFermented() {
		return fermented;
	}

	public final void setFermented(boolean fermented) {
		this.fermented = fermented;
	}

	public final boolean isDistilled() {
		return distilled;
	}

	public final void setDistilled(boolean distilled) {
		this.distilled = distilled;
	}
}
