package com.dreamless.brewery.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CauldronLevelChangeEvent;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.entity.BreweryCauldron;

public class CauldronListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCauldronChange(CauldronLevelChangeEvent event) {
		Block block = event.getBlock();
		if(block.getType() != Material.WATER_CAULDRON)
		{
			return;
		}
		if(event.getNewState().getBlockData().getMaterial() == Material.CAULDRON)
		{
			Brewery.breweryDriver.debugLog("Cauldron emptied");
			BreweryCauldron.remove(event.getBlock());
		} 
		else
		{
			Levelled newBlockState = (Levelled)event.getNewState().getBlockData();
			Brewery.breweryDriver.debugLog("Cauldron is now at level: " + newBlockState.getLevel());
			if (newBlockState.getLevel() == 3) {
				Brewery.breweryDriver.debugLog("Cauldron is refilled, cancelling.");
				BreweryCauldron.remove(event.getBlock());
			}
		}
	}
}
