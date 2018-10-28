package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import de.tr7zw.itemnbtapi.NBTItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Brew {

	// represents the liquid in the brewed Potions

	public static Map<Integer, Brew> potions = new HashMap<Integer, Brew>();
	public static long installTime = System.currentTimeMillis(); // plugin install time in millis after epoch
	public static Boolean colorInBarrels; // color the Lore while in Barrels
	public static Boolean colorInBrewer; // color the Lore while in Brewer

	private float ageTime;
	// Distilling section ---------------

	// distill all custom potions in the brewer
	public static void distillAll(BrewerInventory inv) {
		for(int i = 0; i < 3; i++) {
			ItemStack item = inv.getItem(i);
			if(item != null) {
				NBTItem nbti = new NBTItem(item);
				if(nbti.hasKey("brewery")) {
					PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
					distillSlot(item, potionMeta);
				}
			}
		}
	}

	// distill custom potion in given slot
	public static void distillSlot(ItemStack slotItem, PotionMeta potionMeta) {//TODO Update
		Brewery.breweryDriver.debugLog("Starting to distill");
		//slotItem.setItemMeta(potionMeta);
	}

}
