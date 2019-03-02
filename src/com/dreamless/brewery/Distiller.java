package com.dreamless.brewery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.dreamless.brewery.utils.BreweryMessage;
import com.dreamless.brewery.utils.BreweryUtils;
import com.dreamless.brewery.utils.NBTCompound;
import com.dreamless.brewery.utils.NBTItem;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

public class Distiller implements InventoryHolder {
	
	public static ArrayList<Distiller> distillers = new ArrayList<Distiller>();
	private static final int FILTER_LIMT = 9;
	
	private ArrayList<Material> filters = new ArrayList<Material>(); 
	private Block block;
	private Inventory filterInventory;
	private Hologram hologram;
	private BrewingStand brewingStand;
	
	public Distiller(Block block) {
		this.block = block;
		
		//Initialize Inventory
		filterInventory = org.bukkit.Bukkit.createInventory(this, FILTER_LIMT, "Distiller Filter Cache");
		
		//Hologram
		if(hologram == null) {
			createHologram(block);
		}
		
		distillers.add(this);
	}
	
	
	//Static methods
	public static Distiller get(Block block) {
		for (Distiller distiller : distillers) {
			if (distiller.block.equals(block)) {
				return distiller;
			}
		}
		return null;
	}
	
	public static void add(Distiller distiller) {
		distillers.add(distiller);
	}
	
	public static void remove(Block block) {
		Distiller distiller = get(block);
		if (distiller != null) {
			for(ItemStack item: distiller.filterInventory) {
				distiller.ejectItem(item);
			}
			distillers.remove(distiller);
			
			//Remove hologram
			distiller.hologram.delete();
		}
	}
	
	//Prep?
	public BreweryMessage prepDistiller() {
		if(loadFilters() == 0) {//No filters
			removeSelf();
			return new BreweryMessage(false, "No appropriate filters were loaded.");
		}
		
		ArrayList<String> hologramMessageArrayList = new ArrayList<String>();
		hologramMessageArrayList.add("Distiller ready!");
		hologramMessageArrayList.add(filters.size() + (filters.size() > 1 ? " filters" : " filter") + " loaded");
		
		updateHologram(hologramMessageArrayList, false);
		
		return new BreweryMessage(true, filters.size() + (filters.size() > 1 ? " filters" : " filter") + " loaded into the distiller.");
	}
	
	
	//Load Filters
	private int loadFilters() {
		ItemStack[] filterCache = new ItemStack[9];
		ItemStack[] convertedInventory = filterInventory.getContents();
		int index = 0;
		
		filters.clear();
		
		for(int i = 0; i < convertedInventory.length; i++) {
			ItemStack item = convertedInventory[i];
			
			if(item == null || item.getType() == Material.AIR) {
				continue;
			}
			
			//If you're already full or not valid, eject
			if(index >= FILTER_LIMT || !isValidFilter(item)) {
				//Eject
				ejectItem(item);
		    	continue;
			}
			
			//Add to cache
			filterCache[index++] = new ItemStack(item.getType(), 1);
			filters.add(item.getType());
			Brewery.breweryDriver.debugLog("Filter added: " + item.getType());
			//Reduce item
			i--;
			item.setAmount(item.getAmount()-1);
		}
		filterInventory.setContents(filterCache);
		return filters.size();
	}
	
	private boolean isValidFilter(ItemStack item) {
		switch(item.getType()) {
			case GLOWSTONE_DUST:
			case REDSTONE:
			case GUNPOWDER:
			case SUGAR:
				return true;
			default:
				return false;
		}
	}
	
	private void ejectItem(ItemStack item) {
		if(item != null && item.getType() != Material.AIR)
		block.getWorld().dropItem(block.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5), item);
    	block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP,(float)(Math.random()/2) + 0.75f, (float)(Math.random()/2) + 0.75f);
	}
	//Distilling Handling
	
	
	
	

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
	
	//Inventory handling
	private boolean isEmpty() {
		for(ItemStack it : filterInventory.getContents())	{
		    if(it != null) return false;
		}
		return true;
	}
	
	private void removeSelf() {
		hologram.delete();
		distillers.remove(this);
		Brewery.breweryDriver.debugLog("Check distill list: " + distillers.size());
	}
	
	private void createHologram(Block block) {
		Location above = block.getRelative(BlockFace.UP).getLocation();
		above.setX(above.getX()+ 0.5);
		above.setY(above.getY()+ 0.75);
		above.setZ(above.getZ()+ 0.5);
		hologram = HologramsAPI.createHologram(Brewery.breweryDriver, above);
		
		//Create ready message
		hologram.appendTextLine("Distiller Ready");
		hologram.appendTextLine("Awaiting filters...");
	}
	
	private void updateHologram(ArrayList<String> messages, Boolean distilling) {
		hologram.clearLines();
		if(distilling) {
			hologram.appendItemLine(new ItemStack(filters.get(0)));
		}
		for(String message : messages) {
			hologram.appendTextLine(message);
		}
	}

	@Override
	public Inventory getInventory() {
		return filterInventory;
	}

}
