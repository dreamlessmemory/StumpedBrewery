package com.dreamless.brewery.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;

import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.BreweryDistiller;

public class BlockListener implements Listener {
	
	// Remove Distiller
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBrewingStandBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if(block.getType() != Material.BREWING_STAND) {
			return;
		}
		BreweryDistiller distiller = BreweryDistiller.get(block);
		if(distiller == null) {
			return;
		}
		BreweryDistiller.remove(block);
	}
	
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
