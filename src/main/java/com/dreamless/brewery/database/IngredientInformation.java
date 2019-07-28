package com.dreamless.brewery.database;

import com.dreamless.brewery.recipe.RecipeEnum.Flavor;
import com.dreamless.brewery.recipe.RecipeEnum.IngredientType;

public class IngredientInformation {
	private final IngredientType primaryIngredientType;
	private final IngredientType secondaryIngredientType;
	private final Flavor primaryFlavor;
	private final Flavor secondaryFlavor;
	
	public IngredientInformation(IngredientType primaryIngredientType, IngredientType secondaryIngredientType,
			Flavor primaryFlavor, Flavor secondaryFlavor) {
		this.primaryIngredientType = primaryIngredientType;
		this.secondaryIngredientType = secondaryIngredientType;
		this.primaryFlavor = primaryFlavor;
		this.secondaryFlavor = secondaryFlavor;
	}
	public final IngredientType getPrimaryIngredientType() {
		return primaryIngredientType;
	}
	public final IngredientType getSecondaryIngredientType() {
		return secondaryIngredientType;
	}
	public final Flavor getPrimaryFlavor() {
		return primaryFlavor;
	}
	public final Flavor getSecondaryFlavor() {
		return secondaryFlavor;
	}
}
