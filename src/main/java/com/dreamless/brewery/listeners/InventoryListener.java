package com.dreamless.brewery.listeners;

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
import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.BreweryCauldron;
import com.dreamless.brewery.entity.BreweryDistiller;
import com.dreamless.brewery.player.BPlayer;

public class InventoryListener implements Listener {
	/*
	// Prevent removing items from cauldron that is cooking
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryCauldronOpen(InventoryClickEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof BreweryCauldron)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Cauldron click open");
		BreweryCauldron cauldronIngredientsngredients = (BreweryCauldron) event.getView().getTopInventory().getHolder();
		if(cauldronIngredientsngredients.isCooking()) {
			if(event.isShiftClick() || (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof BreweryCauldron)) {
				event.setCancelled(true);
				event.setResult(Result.DENY);
				Brewery.breweryDriver.debugLog("Cauldron cancelled interaction");
			}
		}
	}*/
	
	/*
	// Prevent removing items from barrel that is aging
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
	}*/
	/*
	// Prevent dragging actions with a cauldron that is cooking
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryCauldronDrag(InventoryDragEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof BreweryCauldron)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Cauldron drag open");
		BreweryCauldron ingredients = (BreweryCauldron) event.getView().getTopInventory().getHolder();
		if(ingredients.isCooking()) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			Brewery.breweryDriver.debugLog("Cauldron cancelled interaction");
		}
	}*/
	
	/*
	// Prevent dragging actions with a barrel that is aging
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
	}*/
	
	// Prevent actions with a Distiller that is active
	/*@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)	
	public void onBreweryDistillerFinished(InventoryClickEvent event) {
		InventoryHolder holder = event.getView().getTopInventory().getHolder();
		if (!(holder instanceof BrewingStand)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Distiller click open");
		BreweryDistiller distiller = BreweryDistiller.get(((BrewingStand)holder).getBlock());
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

	// Actions that fire off when closing an inventory
	@EventHandler
	//FIXME: Doesn't work
	public void onInventoryClose(InventoryCloseEvent event) {
		InventoryHolder holder = event.getInventory().getHolder();
		if (holder instanceof BreweryDistiller) {
			BreweryDistiller distiller = ((BreweryDistiller) holder);
			if(distiller.isFinishedDistilling() && distiller.isEmpty()) {
				distiller.removeSelf();
			}
		} 
	}
}
