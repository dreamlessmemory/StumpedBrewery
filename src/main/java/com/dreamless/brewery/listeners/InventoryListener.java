package com.dreamless.brewery.listeners;

import org.bukkit.Sound;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.entity.BIngredients;
import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.Distiller;
import com.dreamless.brewery.player.BPlayer;
import com.dreamless.brewery.utils.BreweryMessage;

public class InventoryListener implements Listener {
		
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryCauldronOpen(InventoryClickEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof BIngredients)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Cauldron click open");
		BIngredients ingredients = (BIngredients) event.getView().getTopInventory().getHolder();
		if(ingredients.isCooking()) {
			if(event.isShiftClick() || (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof BIngredients)) {
				event.setCancelled(true);
				event.setResult(Result.DENY);
				Brewery.breweryDriver.debugLog("Cauldron cancelled interaction");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryBarrelOpen(InventoryClickEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof BreweryBarrel)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Barrel click open");
		BreweryBarrel barrel = (BreweryBarrel) event.getView().getTopInventory().getHolder();
		if(barrel.isAging()) {
			if(event.isShiftClick() || (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof BreweryBarrel)) {
				event.setCancelled(true);
				event.setResult(Result.DENY);
				Brewery.breweryDriver.debugLog("barrel cancelled interaction");
			}
		}
	}
	
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryCauldronDrag(InventoryDragEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof BIngredients)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Cauldron drag open");
		BIngredients ingredients = (BIngredients) event.getView().getTopInventory().getHolder();
		if(ingredients.isCooking()) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			Brewery.breweryDriver.debugLog("Cauldron cancelled interaction");
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryBarrelDrag(InventoryDragEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof BreweryBarrel)) {
			return;
		}
		Brewery.breweryDriver.debugLog("barrel drag open");
		BreweryBarrel barrel = (BreweryBarrel) event.getView().getTopInventory().getHolder();
		if(barrel.isAging()) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			Brewery.breweryDriver.debugLog("barrel cancelled interaction");
		}
	}
	
	/*@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryDistillerFinished(InventoryClickEvent event) {
		InventoryHolder holder = event.getView().getTopInventory().getHolder();
		if (!(holder instanceof BrewingStand)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Distiller click open");
		Distiller distiller = Distiller.get(((BrewingStand)holder).getBlock());
		if(distiller != null && distiller.isFinishedDistilling()) {
			
			
			
			
			
			if(event.isShiftClick() || (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof BrewerInventory)) {
				event.setCancelled(true);
				event.setResult(Result.DENY);
				Brewery.breweryDriver.debugLog("distiller cancelled interaction");
			}
		}
	}*/

	// block the pickup of items where getPickupDelay is > 1000 (puke)
	@EventHandler(ignoreCancelled = true)
	public void onInventoryPickupItem(InventoryPickupItemEvent event){
		if (event.getItem().getPickupDelay() > 1000 && event.getItem().getItemStack().getType() == BPlayer.pukeItem) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		InventoryHolder holder = event.getInventory().getHolder();
		if (holder instanceof BreweryBarrel) {
			event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
		} else if (holder instanceof BrewingStand) {
			Distiller distiller = Distiller.get(((BrewingStand)holder).getBlock());
			if(distiller == null) {
				return;
			}
			if(distiller.isFinishedDistilling()) {
				Brewery.breweryDriver.debugLog("Finished, check");
				if(distiller.isEmpty()) {
					Brewery.breweryDriver.debugLog("empty");
					distiller.removeSelf();
				}
			}
		} else if (holder instanceof Distiller) {
			Distiller distiller = ((Distiller) holder);
			BreweryMessage breweryMessage = distiller.prepDistiller();
			Brewery.breweryDriver.msg(event.getPlayer(), breweryMessage.getMessage());
		}
	}
}
