package com.dreamless.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CauldronLevelChangeEvent;

import com.dreamless.brewery.entity.Cauldron;

public class CauldronListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCauldronChange(CauldronLevelChangeEvent event) {
		if (event.getNewLevel() == 0 && event.getOldLevel() != 0) {
			if (event.getReason() == CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL) {
				return;
			}
			Cauldron.remove(event.getBlock());
		} else if (event.getNewLevel() == 3 && event.getOldLevel() != 3) {
			Cauldron.remove(event.getBlock());
		}
	}
}
