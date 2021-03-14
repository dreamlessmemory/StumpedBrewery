package com.dreamless.brewery.brew;

import org.bukkit.Material;

import java.util.ArrayList;

public enum Aspect {
	LITHIC, INFERNAL, PYROTIC, AERIAL, VOID, AQUATIC, INVALID;

	public static ArrayList<Aspect> getAspect(Material material) {

		ArrayList<Aspect> aspectList = new ArrayList<Aspect>();

		if(isAerial(material))
		{
			aspectList.add(AERIAL);
		}
		if(isLithic(material))
		{
			aspectList.add(LITHIC);
		}
		if(isAquatic(material))
		{
			aspectList.add(AQUATIC);
		}
		if(isPyrotic(material))
		{
			aspectList.add(PYROTIC);
		}
		if(isVoid(material))
		{
			aspectList.add(VOID);
		}
		if(isInfernal(material))
		{
			aspectList.add(INFERNAL);
		}

		return aspectList;
	}

	public static boolean isAerial(Material material) 
	{
		switch(material)
		{
		case ACACIA_SAPLING:
		case ACACIA_LEAVES:
		case ALLIUM:
		case APPLE:
		case AZURE_BLUET:
		case BEETROOT:
		case BIRCH_LEAVES:
		case BLAZE_POWDER:
		case BLUE_ICE:
		case BLUE_ORCHID:
		case BROWN_MUSHROOM:
		case CARROTS:
		case CHORUS_FRUIT:
		case COAL:
		case COBWEB:
		case COOKED_COD:
		case COOKED_SALMON:
		case DANDELION:
		case DARK_OAK_SAPLING:
		case DRAGON_BREATH:
		case EGG:
		case EMERALD:
		case EMERALD_BLOCK:
		case ENDER_PEARL:
		case FEATHER:
		case GHAST_TEAR:
		case GLASS:
		case GOLD_NUGGET:
		case GOLDEN_APPLE:
		case GRAVEL:
		case HONEY_BOTTLE:
		case HONEY_BLOCK:
		case HONEYCOMB:
		case JUNGLE_SAPLING:
		case KELP:
		case LAPIS_LAZULI:
		case LILAC:
		case LILY_PAD:
		case MILK_BUCKET:
		case OAK_LEAVES:
		case OAK_SAPLING:
		case OXEYE_DAISY:
		case PEONY:
		case PINK_TULIP:
		case POISONOUS_POTATO:
		case PUMPKIN:
		case RABBIT_FOOT:
		case RED_MUSHROOM:
		case RED_TULIP:
		case ROSE_BUSH:
		case SLIME_BALL:
		case SNOWBALL:
		case STICK:
		case SUGAR:
		case SUNFLOWER:
		case SWEET_BERRIES:
		case TALL_GRASS:
		case VINE:
		case WHITE_TULIP:
			return true;
		default:
			return false;
		}
	}

	public static boolean isLithic(Material material) 
	{
		switch(material)
		{
		case ACACIA_LEAVES:
		case ACACIA_SAPLING:
		case ALLIUM:
		case APPLE:
		case BEETROOT:
		case BIRCH_LEAVES:
		case BLUE_ORCHID:
		case BONE:
		case BREAD:
		case CHARCOAL:
		case COAL:
		case COCOA_BEANS:
		case DANDELION:
		case DARK_OAK_LEAVES:
		case DARK_OAK_SAPLING:
		case DEAD_BUSH:
		case DIAMOND:
		case DIAMOND_BLOCK:
		case EMERALD:
		case EMERALD_BLOCK:
		case FERN:
		case GLASS:
		case GLOWSTONE:
		case GLOWSTONE_DUST:
		case GOLD_INGOT:
		case GOLD_NUGGET:
		case GRAVEL:
		case HAY_BLOCK:
		case HONEY_BOTTLE:
		case IRON_INGOT:
		case IRON_BLOCK:
		case IRON_NUGGET:
		case LAPIS_LAZULI:
		case MAGMA_BLOCK:
		case NAUTILUS_SHELL:
		case NETHERRACK:
		case ORANGE_TULIP:
		case OXEYE_DAISY:
		case PEONY:
		case POISONOUS_POTATO:
		case POTATO:
		case PUMPKIN_SEEDS:
		case PURPUR_BLOCK:
		case QUARTZ:
		case QUARTZ_BLOCK:
		case PORKCHOP:
		case RED_MUSHROOM:
		case REDSTONE:
		case ROSE_BUSH:
		case SAND:
		case SHULKER_SHELL:
		case SNOWBALL:
		case SOUL_SAND:
		case SPRUCE_LEAVES:
		case STICK:
		case SUGAR:
		case SUNFLOWER:
		case WHEAT:
		case WHITE_TULIP:
			return true;
		default:
			return false;
		}
	}

	public static boolean isPyrotic(Material material) 
	{
		switch(material)
		{
		case ACACIA_LEAVES:
		case BIRCH_SAPLING:
		case BLAZE_POWDER:
		case BLAZE_ROD:
		case BREAD:
		case CACTUS:
		case CHARCOAL:
		case COAL:
		case COCOA_BEANS:
		case COOKED_COD:
		case COOKED_SALMON:
		case COOKED_PORKCHOP:
		case DEAD_BUSH:
		case DIAMOND:
		case EMERALD:
		case ENDER_EYE:
		case FEATHER:
		case FERN:
		case GLASS:
		case GLISTERING_MELON_SLICE:
		case GOLD_INGOT:
		case GOLD_BLOCK:
		case GOLD_NUGGET:
		case GRAVEL:
		case GUNPOWDER:
		case HAY_BLOCK:
		case IRON_INGOT:
		case IRON_BLOCK:
		case IRON_NUGGET:
		case JUNGLE_LEAVES:
		case JUNGLE_SAPLING:
		case KELP:
		case MAGMA_BLOCK:
		case MAGMA_CREAM:
		case MELON_SLICE:
		case NETHERRACK:
		case OAK_LEAVES:
		case OAK_SAPLING:
		case OBSIDIAN:
		case ORANGE_TULIP:
		case OXEYE_DAISY:
		case PINK_TULIP:
		case POPPED_CHORUS_FRUIT:
		case POPPY:
		case PRISMARINE_SHARD:
		case PUMPKIN:
		case PUMPKIN_SEEDS:
		case QUARTZ:
		case PORKCHOP:
		case RED_TULIP:
		case REDSTONE:
		case ROTTEN_FLESH:
		case SAND:
		case SOUL_SAND:
		case SPRUCE_LEAVES:
		case STICK:
		case SUNFLOWER:
		case WHEAT:
		case WITHER_ROSE:
			return true;
		default:
			return false;
		}
	}

	public static boolean isInfernal(Material material) 
	{
		switch(material)
		{
		case AZURE_BLUET:
		case BEETROOT:
		case BIRCH_SAPLING:
		case BLAZE_POWDER:
		case BLAZE_ROD:
		case BLUE_ORCHID:
		case BONE:
		case BONE_BLOCK:
		case BROWN_MUSHROOM:
		case CACTUS:
		case CARROTS:
		case CHARCOAL:
		case COAL:
		case COBWEB:
		case COCOA_BEANS:
		case DARK_OAK_LEAVES:
		case DARK_OAK_SAPLING:
		case DEAD_BUSH:
		case EGG:
		case FERMENTED_SPIDER_EYE:
		case GHAST_TEAR:
		case GLISTERING_MELON_SLICE:
		case GLOWSTONE:
		case GLOWSTONE_DUST:
		case GOLD_INGOT:
		case GOLD_BLOCK:
		case GOLD_NUGGET:
		case GOLDEN_APPLE:
		case GOLDEN_CARROT:
		case GRAVEL:
		case GUNPOWDER:
		case HONEYCOMB:
		case IRON_NUGGET:
		case JUNGLE_LEAVES:
		case JUNGLE_SAPLING:
		case KELP:
		case LILAC:
		case MAGMA_BLOCK:
		case MAGMA_CREAM:
		case MELON_SLICE:
		case MELON:
		case MILK_BUCKET:
		case NETHER_STAR:
		case NETHERRACK:
		case NETHER_WART:
		case ORANGE_TULIP:
		case PEONY:
		case PINK_TULIP:
		case POISONOUS_POTATO:
		case PRISMARINE_CRYSTALS:
		case PUMPKIN:
		case PUMPKIN_SEEDS:
		case QUARTZ:
		case QUARTZ_BLOCK:
		case RED_TULIP:
		case REDSTONE:
		case ROTTEN_FLESH:
		case SAND:
		case SOUL_SAND:
		case SPIDER_EYE:
		case SWEET_BERRIES:
		case TALL_SEAGRASS:
		case WHEAT:
		case WITHER_ROSE:
			return true;
		default:
			return false;
		}
	}

	public static boolean isAquatic(Material material) 
	{
		switch(material)
		{
		case ACACIA_SAPLING:
		case ALLIUM:
		case APPLE:
		case AZURE_BLUET:
		case BIRCH_LEAVES:
		case BIRCH_SAPLING:
		case BREAD:
		case CACTUS:
		case CARROTS:
		case CHORUS_FLOWER:
		case CHORUS_FRUIT:
		case COOKED_SALMON:
		case COOKED_COD:
		case DANDELION:
		case EGG:
		case END_ROD:
		case FEATHER:
		case GHAST_TEAR:
		case HAY_BLOCK:
		case HONEY_BOTTLE:
		case HONEY_BLOCK:
		case IRON_INGOT:
		case IRON_NUGGET:
		case JUNGLE_LEAVES:
		case JUNGLE_SAPLING:
		case KELP:
		case LILAC:
		case LILY_PAD:
		case MELON_SLICE:
		case MELON:
		case MILK_BUCKET:
		case NAUTILUS_SHELL:
		case NETHERRACK:
		case OAK_LEAVES:
		case OAK_SAPLING:
		case PEONY:
		case PINK_TULIP:
		case PRISMARINE_CRYSTALS:
		case PRISMARINE_SHARD:
		case PUMPKIN:
		case COD:
		case SALMON:
		case TROPICAL_FISH:
		case PORKCHOP:
		case RED_TULIP:
		case ROSE_BUSH:
		case ROTTEN_FLESH:
		case SAND:
		case SLIME_BALL:
		case SLIME_BLOCK:
		case SNOWBALL:
		case SNOW_BLOCK:
		case SPIDER_EYE:
		case SPONGE:
		case SPRUCE_LEAVES:
		case SPRUCE_SAPLING:
		case STICK:
		case SUGAR:
		case SWEET_BERRIES:
		case TALL_SEAGRASS:
		case WHEAT:
		case WHITE_TULIP:
			return true;
		default:
			return false;
		}
	}

	public static boolean isVoid(Material material) 
	{
		switch(material)
		{
		case ACACIA_SAPLING:
		case ALLIUM:
		case AZURE_BLUET:
		case BEETROOT:
		case BLUE_ORCHID:
		case BONE:
		case BONE_BLOCK:
		case BROWN_MUSHROOM:
		case CACTUS:
		case CARROTS:
		case CHORUS_FLOWER:
		case CHORUS_FRUIT:
		case COBWEB:
		case DANDELION:
		case DARK_OAK_LEAVES:
		case DARK_OAK_SAPLING:
		case DRAGON_BREATH:
		case EGG:
		case END_ROD:
		case ENDER_PEARL:
		case ENDER_EYE:
		case FEATHER:
		case FERMENTED_SPIDER_EYE:
		case FERN:
		case GLOWSTONE_DUST:
		case GOLDEN_CARROT:
		case GUNPOWDER:
		case HONEYCOMB:
		case IRON_NUGGET:
		case LAPIS_LAZULI:
		case LARGE_FERN:
		case LILAC:
		case LILY_PAD:
		case MAGMA_BLOCK:
		case MAGMA_CREAM:
		case MELON_SLICE:
		case MELON:
		case MILK_BUCKET:
		case OAK_SAPLING:
		case ORANGE_TULIP:
		case OXEYE_DAISY:
		case PEONY:
		case PINK_TULIP:
		case POISONOUS_POTATO:
		case POPPED_CHORUS_FRUIT:
		case PUMPKIN_SEEDS:
		case PURPUR_BLOCK:
		case RABBIT_FOOT:
		case RED_MUSHROOM:
		case REDSTONE:
		case ROSE_BUSH:
		case ROTTEN_FLESH:
		case SHULKER_SHELL:
		case SLIME_BALL:
		case SLIME_BLOCK:
		case SOUL_SAND:
		case SPIDER_EYE:
		case SUGAR:
		case SUNFLOWER:
		case TALL_SEAGRASS:
		case WHITE_TULIP:
		case WITHER_ROSE:
			return true;
		default:
			return false;
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
