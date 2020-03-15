package com.dreamless.brewery.database;

import com.dreamless.brewery.recipe.RecipeEnum.IngredientType;

@Deprecated
public class BrewTypeRequirement {
	private IngredientType primaryIngredient;
	private IngredientType secondaryIngredient;
	private byte grade;
	
	public BrewTypeRequirement(IngredientType primaryIngredient, IngredientType secondaryIngredient, byte grade) {
		this.primaryIngredient = primaryIngredient;
		this.secondaryIngredient = secondaryIngredient;
		this.grade = grade;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof BrewTypeRequirement)) {
			return false;
		}
		BrewTypeRequirement other = (BrewTypeRequirement)obj;
		return primaryIngredient == other.primaryIngredient && 
				secondaryIngredient == other.secondaryIngredient && 
				grade == other.grade;
	}
}
