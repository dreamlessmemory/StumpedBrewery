package com.dreamless.brewery.fermentation;

import org.bukkit.Material;

public class BreweryIngredient {
	
	public static boolean isValidIngredient (Material material) {
		return Aspect.getAspect(material) != Aspect.INVALID;
	}

	public enum Rarity {
		COMMON, COMMON_REFINED, UNCOMMON, UNCOMMON_REFINED, RARE, RARE_REFINED, LEGENDARY, INVALID;
		
		public final int getCookTime() {
			switch(this) {
			case COMMON:
				return 1;
			case COMMON_REFINED:
			case UNCOMMON:
				return 2;
			case UNCOMMON_REFINED:
			case RARE:
				return 3;
			case RARE_REFINED:
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
			case COMMON_REFINED:
				return 2;
			case UNCOMMON:
				return 3;
			case UNCOMMON_REFINED:
				return 4;
			case RARE:
				return 5;
			case RARE_REFINED:
				return 6;
			case LEGENDARY:
				return 7;
			default:
				return 0;
			}	
		}
		
		public final int getSaturation() {
			switch(this) {
			case COMMON:
				return 10;
			case COMMON_REFINED:
				return 20;
			case UNCOMMON:
				return 30;
			case UNCOMMON_REFINED:
				return 40;
			case RARE:
				return 50;
			case RARE_REFINED:
				return 60;
			case LEGENDARY:
				return 70;
			default:
				return 0;
			}	
		}
		
		public static Rarity getRarity(Material material) {
			switch(material) {
			case OAK_LEAVES:
			case OAK_SAPLING:
				return COMMON;
			default:
				return INVALID;
			}
		}
	}
	public enum Aspect {
		LITHIC, INFERNAL, PYROTIC, AERIAL, VOID, AQUATIC, INVALID;
		
		public static Aspect getAspect(Material material) {
			switch(material) {
			case OAK_LEAVES:
			case OAK_SAPLING:
				return AERIAL;
			default:
				return INVALID;
			}
		}
	}
	
	public static class PairedAspectRarity	{
		public final Aspect aspect;
		public final Rarity rarity;
		
		public PairedAspectRarity(Aspect aspect, Rarity rarity) {
			this.aspect = aspect;
			this.rarity = rarity;
		}
	}
	
}
