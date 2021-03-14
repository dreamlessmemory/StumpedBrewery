package com.dreamless.brewery.brew;

import org.bukkit.Material;

public enum Rarity {
	COMMON, UNCOMMON, RARE, LEGENDARY, INVALID;
	
	public final int getCookTime() {
		switch(this) {
		case COMMON:
			return 3; //3 
		case UNCOMMON:
			return 4; //4
		case RARE:
			return 6; //6
		case LEGENDARY:
			return 8;
		default:
			return 0;
		}	
	}
	
	public final double getSaturationBonus() {
		switch(this) {
		case COMMON:
			return 0;
		case UNCOMMON:
			return 0.04;
		case RARE:
			return 0.1;
		case LEGENDARY:
			return 0.4;
		default:
			return 0;
		}	
	}
	
	//TODO: Finalize
	public final double getSaturationCap() {
		switch(this) {
		case COMMON:
			return 0;
		case UNCOMMON:
			return 0.25;
		case RARE:
			return 0.35;
		case LEGENDARY:
			return 0.4;
		default:
			return 0;
		}	
	}
	
	public final int getItemContribution()
	{
		switch(this) {
		case COMMON:
			return 10;
		case UNCOMMON:
			return 5;
		case RARE:
			return 3;
		case LEGENDARY:
			return 1;
		default:
			return 0;
		}
	}
	
	public static Rarity getRarity(Material material) {
		switch(material) {
		case OAK_LEAVES:
		case OAK_SAPLING:
		case SPRUCE_LEAVES:
		case SPRUCE_SAPLING:
		case BIRCH_LEAVES:
		case BIRCH_SAPLING:
		case JUNGLE_LEAVES:
		case JUNGLE_SAPLING:
		case ACACIA_LEAVES:
		case ACACIA_SAPLING:
		case DARK_OAK_LEAVES:
		case DARK_OAK_SAPLING:
		case SAND:
		case GRAVEL:
		case COAL:
		case DANDELION:
		case POPPY:
		case BLUE_ORCHID:
		case ALLIUM:
		case AZURE_BLUET:
		case RED_TULIP:
		case ORANGE_TULIP:
		case PINK_TULIP:
		case OXEYE_DAISY:
		case SUNFLOWER:
		case LILAC:
		case ROSE_BUSH:
		case PEONY:
		case GOLD_NUGGET:
		case IRON_NUGGET:
		case REDSTONE:
		case SUGAR:
		case CACTUS:
		case PUMPKIN:
		case PUMPKIN_SEEDS:
		case NETHERRACK:
		case SOUL_SAND:
		case MELON_SLICE:
		case MAGMA_BLOCK:
		case CARROTS:
		case POTATOES:
		case BEETROOTS:
		case WHEAT:
		case STICK:
		case FEATHER:
		case EGG:
		case COD:
		case TROPICAL_FISH:
		case SALMON:
		case ROTTEN_FLESH:
		case POISONOUS_POTATO:
		case MILK_BUCKET:
		case KELP:
			return COMMON;
		case CHARCOAL:
		case COBWEB:
		case COOKED_SALMON:
		case COOKED_COD:
		case GLASS:
		case DEAD_BUSH:
		case GRASS:
		case TALL_GRASS:
		case TALL_SEAGRASS:
		case FERN:
		case LARGE_FERN:
		case BROWN_MUSHROOM:
		case RED_MUSHROOM:
		case GOLD_INGOT:
		case IRON_INGOT:
		case EMERALD:
		case QUARTZ:
		case SNOWBALL:
		case GLOWSTONE_DUST:
		case MELON:
		case VINE:
		case LILY_PAD:
		case NETHER_WART:
		case MAGMA_CREAM:
		case CHORUS_FRUIT:
		case COCOA_BEANS:
		case SLIME_BALL:
		case BREAD:
		case HAY_BLOCK:
		case BONE:
		case APPLE:
		case GUNPOWDER:
		case PORKCHOP:
		case ENDER_PEARL:
		case BLAZE_POWDER:
		case HONEY_BOTTLE:
		case HONEYCOMB:
		case LAPIS_LAZULI:
		case SWEET_BERRIES:
			return UNCOMMON;
		case SNOW_BLOCK:
		case POPPED_CHORUS_FRUIT:
		case BONE_BLOCK:
		case COOKED_PORKCHOP:
		case HONEY_BLOCK:
		case GOLD_BLOCK:
		case IRON_BLOCK:
		case DIAMOND:
		case GLOWSTONE:
		case CHORUS_FLOWER:
		case END_ROD:
		case NAUTILUS_SHELL:
		case SLIME_BLOCK:
		case PRISMARINE_SHARD:
		case PRISMARINE_CRYSTALS:
		case GOLDEN_APPLE:
		case BLAZE_ROD:
		case GHAST_TEAR:
		case FERMENTED_SPIDER_EYE:
		case ENDER_EYE:
		case GLISTERING_MELON_SLICE:
		case GOLDEN_CARROT:
		case RABBIT_FOOT:
		case SHULKER_SHELL:
			return RARE;
		case SPONGE:
		case DIAMOND_BLOCK:
		case OBSIDIAN:
		case DRAGON_BREATH:
		case BLUE_ICE:
		case NETHER_STAR:
		case WITHER_ROSE:
			return LEGENDARY;
		default:
			return INVALID;
		}
	}
	
	public static boolean isValidIngredient(Material material) {
		return getRarity(material) != INVALID;
	}
}
