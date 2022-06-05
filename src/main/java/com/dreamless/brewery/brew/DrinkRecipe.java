package com.dreamless.brewery.brew;

import org.bukkit.Material;

public class DrinkRecipe {
	
	private static final int SERVINGS_PER_INGREDIENT = 3;
	private static final int PROOF_PER_ITEM = 20;
	private static final int MAX_PROOF = 100;
	
	private final Material primaryIngredient;
	private final Material secondaryIngredient;
	private final Material flavorIngredient;
	private int numberOfServings;
	private int alcoholLevel;
	
	public DrinkRecipe(Material primary, Material secondary, Material flavor)
	{
		primaryIngredient = primary;
		secondaryIngredient = secondary;
		flavorIngredient = flavor;
	}
}
