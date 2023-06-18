package com.dreamless.brewery.brew;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.data.NBTConstants;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BarrelLidItem {
	
	private static final int LID_CREATION_TIME = 20 * 2;
	private static final int LID_EXP = 8;
	
	public static void registerRecipes() {
		Bukkit.addRecipe(lidRecipeFactory(Material.OAK_TRAPDOOR, "brewery_oak_lid", "Oak Lid", BarrelType.OAK, "Barrel_Oak_Lid"));
		Bukkit.addRecipe(lidRecipeFactory(Material.DARK_OAK_TRAPDOOR, "brewery_dark_oak_lid", "Dark Oak Lid", BarrelType.DARK_OAK, "Barrel_Dark_Oak_Lid"));
		Bukkit.addRecipe(lidRecipeFactory(Material.BIRCH_TRAPDOOR, "brewery_birch_lid", "Birch Lid", BarrelType.BIRCH, "Barrel_Birch_Lid"));
		Bukkit.addRecipe(lidRecipeFactory(Material.SPRUCE_TRAPDOOR, "brewery_spruce_lid", "Spruce Lid", BarrelType.SPRUCE, "Barrel_Spruce_Lid"));
		Bukkit.addRecipe(lidRecipeFactory(Material.JUNGLE_TRAPDOOR, "brewery_jungle_lid", "Jungle Wood Lid", BarrelType.JUNGLE, "Barrel_Jungle_Lid"));
		Bukkit.addRecipe(lidRecipeFactory(Material.ACACIA_TRAPDOOR, "brewery_acacia_lid", "Acacia Lid", BarrelType.ACACIA, "Barrel_Acacia_Lid"));
	}

	private static ItemStack lidItemFactory(Material type, String displayname, BarrelType barrelType, String flavortext) {
		ItemStack itemStack = new ItemStack(type);
		ItemMeta meta = itemStack.getItemMeta();
		
		meta.setDisplayName(displayname);
		meta.setLore(BreweryUtils.wordWrap(Brewery.getText(flavortext)));
		
		itemStack.setItemMeta(meta);
		
		NBTItem nbti = new NBTItem(itemStack);
		
		NBTCompound nbtCompound = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING);
		nbtCompound.setString(NBTConstants.WOOD_TYPE_TAG_STRING, barrelType.name());
		
		itemStack = nbti.getItem();
		
		return itemStack;
	}
	
	private static FurnaceRecipe lidRecipeFactory(Material lidtype, String keyname, String displayname, BarrelType barrelType, String flavortext) {
		NamespacedKey key = new NamespacedKey(Brewery.breweryDriver, keyname);
		FurnaceRecipe recipe = new FurnaceRecipe(key, lidItemFactory(lidtype, displayname, barrelType, flavortext), lidtype, LID_CREATION_TIME, LID_EXP);				
		return recipe;
	}
	
	public static boolean isALid(ItemStack item) {
		if(item == null) {
			return false;
		}
		switch(item.getType()) {
		case OAK_TRAPDOOR:
		case DARK_OAK_TRAPDOOR:
		case BIRCH_TRAPDOOR:
		case SPRUCE_TRAPDOOR:
		case JUNGLE_TRAPDOOR:
		case ACACIA_TRAPDOOR:
			return checkNBT(item);
		default:
				return false;
		}
	}
	
	private static boolean checkNBT(ItemStack item) {
		NBTItem nbtItem = new NBTItem(item);
		NBTCompound nbtCompound = nbtItem.getCompound(NBTConstants.BREWERY_TAG_STRING);
		if(nbtCompound == null) {
			return false;
		}
		return nbtCompound.hasTag(NBTConstants.WOOD_TYPE_TAG_STRING);
	}
	
	public static BarrelType getBarrelType(ItemStack item) {
		if(item == null || !isALid(item)) {
			return null;
		}
		NBTItem nbtItem = new NBTItem(item);
		NBTCompound nbtCompound = nbtItem.getCompound(NBTConstants.BREWERY_TAG_STRING);
		if(nbtCompound == null || !nbtCompound.hasTag(NBTConstants.WOOD_TYPE_TAG_STRING)) {
			return null;
		}
		return BarrelType.valueOf(nbtCompound.getString(NBTConstants.WOOD_TYPE_TAG_STRING));
	}
	
}
