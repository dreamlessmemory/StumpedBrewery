package com.dreamless.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.BreweryCauldron;
import com.dreamless.brewery.filedata.DataSave;

public class WorldListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		DataSave.save();
		BreweryBarrel.onUnload(event.getWorld().getName());
		BreweryCauldron.onUnload(event.getWorld().getName());
	}

}
