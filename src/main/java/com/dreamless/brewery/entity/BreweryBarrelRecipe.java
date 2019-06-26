package com.dreamless.brewery.entity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import com.dreamless.brewery.Brewery;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BreweryBarrelRecipe {
	
	public static void registerRecipes() {
		Bukkit.addRecipe(testBarrelRecipe());
	}
	
	public static ItemStack testBarrelItem() {
		ItemStack item = new ItemStack(Material.BARREL);

		/*** Item Meta ***/
		ItemMeta itemMeta = item.getItemMeta();

		// Set Name
		itemMeta.setDisplayName("TEST");

		// Set flavor text
		//itemMeta.setLore("TEST");

		// Apply meta
		item.setItemMeta(itemMeta);

		/*** NBT ***/
		NBTItem nbti = new NBTItem(item);

		NBTCompound laithorn = nbti.addCompound("Brewery");
		laithorn.setString("BarrelType", "BASIC");

		item = nbti.getItem();

		return item;
	}

	private static ShapelessRecipe testBarrelRecipe() {
		
		ItemStack bonemeal =  testBarrelItem();
		NamespacedKey key = new NamespacedKey(Brewery.breweryDriver, "testbarrel");

		ShapelessRecipe recipe = new ShapelessRecipe(key, bonemeal);

		recipe.addIngredient(Material.FLINT);
		recipe.addIngredient(Material.BARREL);

		return recipe;
	}
}
