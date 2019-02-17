package com.dreamless.brewery;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.dreamless.brewery.utils.NBTCompound;
import com.dreamless.brewery.utils.NBTItem;

public class Distiller {

	// distill all custom potions in the brewer
	public static void distillAll(BrewerInventory inv) {
		for(int i = 0; i < 3; i++) {
			ItemStack item = inv.getItem(i);
			if(item != null) {
				NBTItem nbti = new NBTItem(item);
				if(nbti.hasKey("brewery")) {
					NBTCompound brewery = nbti.getCompound("brewery");
					/*if(brewery.hasKey("finishedDistilling")) {
						continue;
					}*/
					
					NBTCompound distilling = brewery.hasKey("distilling") ? brewery.getCompound("distilling") : brewery.addCompound("distilling");
					int cycles = distilling.hasKey("cycles") ? distilling.getInteger("cycles") : 0; 
					if(cycles >= 10) { //You only get 10 cycles
						//TODO Add effects?
						continue;
					}
					//Assign age now
					distilling.setInteger("cycles", ++cycles);
					item = nbti.getItem();
					//Brewery.breweryDriver.debugLog("Cycles is " + cycles);
					item = distillSlot(item, inv.getIngredient());
					
					inv.setItem(i, item);
				}
			}
		}
	}

	// distill custom potion in given slot
	public static ItemStack distillSlot(ItemStack item, ItemStack filter) {//TODO Update
		Brewery.breweryDriver.debugLog("DISTILLING 1 CYCLE : " + item.toString() + " FILTER: " + filter.toString());
		
		//Pull NBT
		NBTItem nbti = new NBTItem(item);
		NBTCompound brewery = nbti.getCompound("brewery");
		NBTCompound distilling = brewery.getCompound("distilling");
		
		//Pull aspects
		NBTCompound aspectList = brewery.getCompound("aspectsActivation");
		Set<String> aspects = aspectList.getKeys();
		
		//Calculate new aspects
		int cycles = distilling.getInteger("cycles");
		for(String currentAspect : aspects) {
			double aspectPotency = aspectList.getDouble(currentAspect);
			double newPotency = Aspect.processFilter(currentAspect, brewery.getString("type"), aspectPotency, filter.getType());
			Brewery.breweryDriver.debugLog("Update Potency of " + currentAspect + ": " + aspectPotency + " -> " + newPotency);
			//Update NBT
			aspectList.setDouble(currentAspect, newPotency);
		}
		
		item = nbti.getItem();
		
		//Mask as Aging Brew
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		potionMeta.setDisplayName("#Distilling Brew");
		ArrayList<String> distilledFlavorText = new ArrayList<String>();
		if(!distilling.hasKey("isDistilling") && distilling.getBoolean("isDistilling") != true) {
			distilling.setBoolean("isDistilling", true);
			item = nbti.getItem();

			distilledFlavorText.add("A distilling " +  brewery.getString("type").toLowerCase() + " brew.");
			distilledFlavorText.add("This brew has distilled for " + cycles + (cycles > 1 ? " cycles" : " cycle"));
		} else {
			//Update flavor text
			List<String> flavorText = potionMeta.getLore();
			distilledFlavorText.add(flavorText.get(0));
			distilledFlavorText.add("This brew has distilled for " + cycles + " cycles");
		}
		
		potionMeta.setLore(distilledFlavorText);
		potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(potionMeta);
		
		return item;
	}

}
