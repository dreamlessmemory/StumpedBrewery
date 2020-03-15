package com.dreamless.brewery.recipe;

import org.bukkit.Material;

public class BreweryIngredient {

	public enum Rarity {
		COMMON, UNCOMMON, RARE, LEGENDARY, INVALID;
		
		public final int getCookTime() {
			switch(this) {
			case COMMON:
				return 1;
			case UNCOMMON:
				return 2;
			case RARE:
				return 3;
			case LEGENDARY:
				return 4;
			default:
				return 0;
			}	
		}
		
		public final int getValue() {
			switch(this) {
			case COMMON:
				return 1;
			case UNCOMMON:
				return 2;
			case RARE:
				return 3;
			case LEGENDARY:
				return 4;
			default:
				return 0;
			}	
		}
		
		public static Rarity getRarity(Material material) {
			switch(material) {
			default:
				return INVALID;
			}
		}
	}
	public enum Aspect {
		LITHIC, INFERNAL, PYROTIC, AERIAL, VOID, AQUATIC, INVALID;
		
		public static Aspect getAspect(Material material) {
			switch(material) {
			default:
				return INVALID;
			}
		}
	}
}
