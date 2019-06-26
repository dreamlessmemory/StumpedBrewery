package com.dreamless.brewery.listeners;

import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.BreweryBarrel.BarrelType;
import com.dreamless.brewery.entity.Distiller;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BlockListener implements Listener {
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBrewingStandBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if(block.getType() != Material.BREWING_STAND) {
			return;
		}
		Distiller distiller = Distiller.get(block);
		if(distiller == null) {
			return;
		}
		Distiller.remove(block);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBarrelBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if(block.getType() != Material.BARREL) {
			return;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		//Brewery.breweryDriver.blockDestroy(event.getBlock(), null);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBarrelPlace(BlockPlaceEvent event) {
		Block placedBlock = event.getBlock();
		if(placedBlock.getType() != Material.BARREL) {
			Brewery.breweryDriver.debugLog("FAIL");
			return;
		}
		ItemStack item = event.getItemInHand();
		
		NBTItem nbti = new NBTItem(item);
		NBTCompound compound = nbti.getCompound("Brewery");
		if(compound != null && compound.hasKey("BarrelType")) {
			new BreweryBarrel((Barrel) placedBlock.getState(), BarrelType.valueOf(compound.getString("BarrelType")), 0, false);
		} else {
			Brewery.breweryDriver.debugLog("FAIL2");
		}
	}
}
