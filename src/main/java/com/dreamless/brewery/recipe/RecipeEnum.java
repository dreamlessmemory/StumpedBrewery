package com.dreamless.brewery.recipe;

@Deprecated
public class RecipeEnum {
	public enum Aspect {
		LITHIC, INFERNAL, PYROTIC, AERIAL, VOID, AQUATIC;
	}
	public enum IngredientType {
		GRAIN, STARCH, APPLE, FRUIT, COCOA, NETHER, END, STONE, GRASS, FLORAL, MEAT, AQUATIC;
		
		public final int getCookTimeIncrease() {
			// TODO: Stub
			return 0;
		}
		
		public final int getCookTimeVariance() {
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
		BITTER, SWEET, SOUR, SALTY, SAVORY, SPICY;
		
		public final Aspect getAssociatedAspect() {
			switch (this) {
			case BITTER:
				return Aspect.LITHIC;
			case SALTY:
				return Aspect.AERIAL;
			case SAVORY:
				return Aspect.PYROTIC;
			case SOUR:
				return Aspect.VOID;
			case SPICY:
				return Aspect.INFERNAL;
			case SWEET:
				return Aspect.AQUATIC;
			default:
				return Aspect.LITHIC;
			}
		}
	}
}
