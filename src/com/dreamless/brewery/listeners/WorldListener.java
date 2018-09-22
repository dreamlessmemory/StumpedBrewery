package com.dreamless.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.dreamless.brewery.BCauldron;
import com.dreamless.brewery.Barrel;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.filedata.DataSave;

import org.bukkit.World;

public class WorldListener implements Listener {

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		World world = event.getWorld();

		if (world.getName().startsWith("DXL_")) {
			Brewery.breweryDriver.loadWorldData(Brewery.breweryDriver.getDxlName(world.getName()), world);
		} else {
			Brewery.breweryDriver.loadWorldData(world.getUID().toString(), world);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		DataSave.save(true);
		Barrel.onUnload(event.getWorld().getName());
		BCauldron.onUnload(event.getWorld().getName());
	}

}
