package com.dreamless.brewery.listeners;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;

import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.BreweryCauldron;

public class BlockListener implements Listener {

	// Remove Barrels
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBarrelBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if(block.getType() == Material.BARREL)
		{
			BreweryBarrel barrel = BreweryBarrel.getBarrel(block);
			if(barrel != null) {
				barrel.removeSelf();
				block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().getX() + 0.5,
						block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
				block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,
						(float) (Math.random() / 8) + 0.1f, (float) (Math.random() / 2) + 0.75f);
			}
		}
		else if (block.getType() == Material.WATER_CAULDRON)
		{
			BreweryCauldron cauldron = BreweryCauldron.get(block);
			if(cauldron != null)
			{
				cauldron.removeSelf();
				block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().getX() + 0.5,
						block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
				block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,
						(float) (Math.random() / 8) + 0.1f, (float) (Math.random() / 2) + 0.75f);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		Block block = event.getBlock();
		if(block.getType() != Material.BARREL) {
			return;
		}

		BreweryBarrel barrel = BreweryBarrel.getBarrel(block);
		if(barrel != null) {
			barrel.removeSelf();
			block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().getX() + 0.5,
					block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
			block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,
					(float) (Math.random() / 8) + 0.1f, (float) (Math.random() / 2) + 0.75f);
		}
	}
}
