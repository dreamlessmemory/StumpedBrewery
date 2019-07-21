package com.dreamless.brewery.recipe;

public class RecipeEnum {
	public enum Aspect {
		LITHIC, INFERNAL, PYROTIC, AERIAL, VOID, AQUATIC;
	}
	public enum IngredientType {
		GRAIN, STARCH, APPLE, FRUIT, COCOA, NETHER, END, STONE, GRASS, FLORAL, SEAFOOD, AQUATIC;
		
		public final int getCookTimeIncrease() {
			// TODO: Stub
			return 0;
		}
		
		public final Aspect getAssociatedAspect() {
			switch (this) {
				default:
					return Aspect.LITHIC;
			}
		}
	}
	public enum Flavor {
		BITTER, SPICY, SAVORY, SWEET, SOUR, SALTY;
	}
}
