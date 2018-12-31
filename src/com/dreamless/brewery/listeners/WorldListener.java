package com.dreamless.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import com.dreamless.brewery.BCauldron;
import com.dreamless.brewery.Barrel;
import com.dreamless.brewery.filedata.DataSave;

public class WorldListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		DataSave.save();
		Barrel.onUnload(event.getWorld().getName());
		BCauldron.onUnload(event.getWorld().getName());
	}

}
