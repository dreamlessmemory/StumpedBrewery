package com.dreamless.brewery.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;

import com.dreamless.brewery.BPlayer;
import com.dreamless.brewery.Barrel;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.Distiller;
import com.dreamless.brewery.Words;
import com.dreamless.brewery.Distiller.DistillerRunnable;

import org.bukkit.event.block.BlockBreakEvent;

public class BlockListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		String[] lines = event.getLines();

		if (lines[0].equalsIgnoreCase("Barrel") || lines[0].equalsIgnoreCase(Brewery.breweryDriver.languageReader.get("Etc_Barrel"))) {
			Player player = event.getPlayer();
			if (!player.hasPermission("brewery.createbarrel.small") && !player.hasPermission("brewery.createbarrel.big")) {
				Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Perms_NoBarrelCreate"));
				return;
			}
			if (Barrel.create(event.getBlock(), player)) {
				Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Player_BarrelCreated"));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onSignChangeLow(SignChangeEvent event) {
		if (Words.doSigns) {
			if (BPlayer.hasPlayer(event.getPlayer())) {
				Words.signWrite(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!Brewery.breweryDriver.blockDestroy(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}
	
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

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		Brewery.breweryDriver.blockDestroy(event.getBlock(), null);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (event.isSticky()) {
			for(Block block : event.getBlocks()) {
				if (Barrel.get(block) != null) {
					event.setCancelled(true);
					break;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		for (Block block : event.getBlocks()) {
			if (Barrel.get(block) != null) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
