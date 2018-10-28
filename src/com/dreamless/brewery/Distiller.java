package com.dreamless.brewery;

import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import de.tr7zw.itemnbtapi.NBTItem;

public class Distiller {

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
