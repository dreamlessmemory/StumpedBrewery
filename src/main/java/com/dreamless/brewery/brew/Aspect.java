package com.dreamless.brewery.brew;

import org.bukkit.Material;

public enum Aspect {
	LITHIC, INFERNAL, PYROTIC, AERIAL, VOID, AQUATIC, INVALID;
	
	public static Aspect getAspect(Material material) {
		switch(material) {
		case OAK_SAPLING:
		case SPRUCE_SAPLING:
		case BIRCH_SAPLING:
		case ACACIA_SAPLING:
		case DARK_OAK_SAPLING:
			return AERIAL;
		default:
			return INVALID;
		}
	}
	
	public static Aspect getFilterAspect(Material material) {
		switch(material) {
		case DIORITE:
			return AERIAL;
		case GRANITE:
			return LITHIC;
		case ANDESITE:
			return INFERNAL;
		case GRAVEL:
			return PYROTIC;
		case SAND:
			return AQUATIC;
		case COBBLESTONE:
			return VOID;
		default:
			return INVALID;
		}
	}
}
