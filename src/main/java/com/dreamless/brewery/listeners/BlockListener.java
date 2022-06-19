package com.dreamless.brewery.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;

import com.dreamless.brewery.entity.BreweryBarrel;

public class BlockListener implements Listener {
		
	// Remove Barrels
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBarrelBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if(block.getType() != Material.BARREL) {
			return;
		}
		
		BreweryBarrel barrel = BreweryBarrel.getBarrel(block);
		if(barrel != null) {
			barrel.removeSelf();
			// TODO: drop items, flood area
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		//Brewery.breweryDriver.blockDestroy(event.getBlock(), null);
		Block block = event.getBlock();
		if(block.getType() != Material.BARREL) {
			return;
		}
		
		BreweryBarrel barrel = BreweryBarrel.getBarrel(block);
		if(barrel != null) {
			barrel.removeSelf();
		}
	}
}
