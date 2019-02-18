package com.dreamless.brewery.listeners;

//import com.dre.brewery.integration.LogBlockBarrel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.dreamless.brewery.*;
import com.dreamless.brewery.utils.NBTCompound;
import com.dreamless.brewery.utils.NBTItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Updated for 1.9 to replicate the "Brewing" process for distilling.
 * Because of how metadata has changed, the brewer no longer triggers as previously described.
 * So, I've added some event tracking and manual forcing of the brewing "animation" if the
 *  set of ingredients in the brewer can be distilled.
 * Nothing here should interfere with vanilla brewing.
 *
 * @author ProgrammerDan (1.9 distillation update only)
 */
public class InventoryListener implements Listener {

	/* === Recreating manually the prior BrewEvent behavior. === */
	private HashSet<UUID> trackedBrewmen = new HashSet<UUID>();
	private HashMap<Block, Integer> trackedBrewers = new HashMap<Block, Integer>();
	private static final int DISTILLTIME = 400;

	/**
	 * Start tracking distillation for a person when they open the brewer window.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerOpen(InventoryOpenEvent event) {
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

		//Brewery.breweryDriver.debugLog("Starting brew inventory tracking");
		trackedBrewmen.add(player.getUniqueId());
	}

	/**
	 * Stop tracking distillation for a person when they close the brewer window.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClose(InventoryCloseEvent event) {
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

		//Brewery.breweryDriver.debugLog("Stopping brew inventory tracking");
		trackedBrewmen.remove(player.getUniqueId());
	}

	/***
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerDrag(InventoryDragEvent event) {
		// Workaround the Drag event when only clicking a slot
		if (event.getInventory() instanceof BrewerInventory) {
			onBrewerClick(new InventoryClickEvent(event.getView(), InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PLACE_ALL));
		} else if (event.getInventory().getHolder() instanceof Barrel) {
			onTransferToBrewer(new InventoryClickEvent(event.getView(), InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PLACE_ALL));
		}
	}***/

	/**
	 * Clicking can either start or stop the new brew distillation tracking.
	 * Note that server restart will halt any ongoing brewing processes and
	 * they will _not_ restart until a new click event.
	 *
	 * @param event the Click event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClick(InventoryClickEvent event) {
		HumanEntity player = event.getWhoClicked();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

		UUID puid = player.getUniqueId();
		if (!trackedBrewmen.contains(puid)) return;

		if (InventoryType.BREWING != inv.getType()) return;
		if (event.getAction() == InventoryAction.NOTHING) return; // Ignore clicks that do nothing

		BrewerInventory brewer = (BrewerInventory) inv;
		final Block brewery = brewer.getHolder().getBlock();

		// If we were already tracking the brewer, cancel any ongoing event due to the click.
		Integer curTask = trackedBrewers.get(brewery);
		if (curTask != null) {
			Bukkit.getScheduler().cancelTask(curTask); // cancel prior
			brewer.getHolder().setBrewingTime(0); // Fixes brewing continuing without fuel for normal potions
		}
		final int fuel = brewer.getHolder().getFuelLevel();

		// Now check if we should bother to track it.
		trackedBrewers.put(brewery, new BukkitRunnable() {
			private int runTime = -1;
			private int brewTime = -1;
			@Override
			public void run() {
				BlockState now = brewery.getState();
				if (now instanceof BrewingStand) {
					BrewingStand stand = (BrewingStand) now;
					if (brewTime == -1) { // only check at the beginning (and end) for distillables
						switch (hasCustom(stand.getInventory())) {
							case 1:
								// Custom potion but not for distilling. Stop any brewing and cancel this task
								if (stand.getBrewingTime() > 0) {
									// Brewing time is sent and stored as short
									// This sends a negative short value to the Client
									// In the client the Brewer will look like it is not doing anything
									stand.setBrewingTime(Short.MAX_VALUE << 1);
									stand.setFuelLevel(fuel);
								}
							case 0:
								// No custom potion, cancel and ignore
								this.cancel();
								trackedBrewers.remove(brewery);
								Brewery.breweryDriver.debugLog("nothing to distill");
								return;
							default:
								runTime = getLongestDistillTime(stand.getInventory());
								brewTime = runTime;
								Brewery.breweryDriver.debugLog("using brewtime: " + runTime);

						}
					}

					brewTime--; // count down.
					stand.setBrewingTime((int) ((float) brewTime / ((float) runTime / (float) DISTILLTIME)) + 1);

					if (brewTime <= 1) { // Done!
						BrewerInventory brewer = stand.getInventory();
						stand.getBlock().getWorld().playSound(stand.getBlock().getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 2.0f, 1.0f);
						if (!runDistill(brewer)) {
							this.cancel();
							trackedBrewers.remove(brewery);
							stand.setBrewingTime(0);
							Brewery.breweryDriver.debugLog("All done distilling");
						} else {
							brewTime = -1; // go again.
							stand.setBrewingTime(0);
							Brewery.breweryDriver.debugLog("Can distill more! Continuing.");
						}
					}
				} else {
					this.cancel();
					trackedBrewers.remove(brewery);
					Brewery.breweryDriver.debugLog("The block was replaced; not a brewing stand.");
				}
			}
		}.runTaskTimer(Brewery.breweryDriver, 2L, 1L).getTaskId());
	}


	private byte hasCustom(BrewerInventory brewer) {
		ItemStack item = brewer.getIngredient(); // ingredient
		boolean hasFilter = hasFilter(item); // need dust in the top slot.
		byte customFound = 0;
		if(containsDistillable(brewer)) {
			if(hasFilter) {
				customFound = 2;
			} else {
				customFound = 1;
			}
		}
		
		//Does not contain distillable = 0
		//Contains distillable,  has filter = 2
		//Contains distillable, no filter = 1
		return customFound;
	}
	
	private boolean hasFilter(ItemStack item) {
		if(item == null) return false;
		switch(item.getType()) {
			case GLOWSTONE_DUST:
			case REDSTONE:
			case SUGAR:
			case GUNPOWDER:
				return true;
			default: return false;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBrew(BrewEvent event) {
		if (hasCustom(event.getContents()) != 0) {
			event.setCancelled(true);
		}
	}

	//Check if it contains a brew, then distill it.
	//If it contains a brew, return true
	private boolean runDistill(BrewerInventory inv) {
		if (containsDistillable(inv)) {
			Distiller.distillAll(inv);
			return true;
		}
		return false;
	}
	
	private boolean containsDistillable(BrewerInventory inv) {
		ItemStack item;
		for(int i = 0; i < 3; i++) {
			item = inv.getItem(i);
			if(item != null) {
				NBTItem nbti = new NBTItem(item);
				if(nbti.hasKey("brewery")) {
					return true;
				}
			}
		}
		return false;
	}

	private int getLongestDistillTime(BrewerInventory inv) {
		return 800;
	}
	/***
	Check if a brew is transferred into a brewing stand or barrel
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTransferToBrewer(InventoryClickEvent event) {
		ItemStack item = null;
		//Check if we're dealing with a barrel or a brewing stand
		InventoryView invView = event.getView();
		Inventory topInventory = invView.getTopInventory();
		InventoryHolder topHolder = topInventory.getHolder();
		if(!(topHolder instanceof Barrel) && !(topHolder instanceof BrewingStand)) {
			//Brewery.breweryDriver.debugLog("Ignoring, neither barrel nor brewing stand.");
			//Brewery.breweryDriver.debugLog(topHolder.toString());
			return;//Not those two types, then ignore.
		}
		
		//Check action, leave if not the correct action to place into a brewing equipment
		InventoryAction action = event.getAction();
		if(action == InventoryAction.PLACE_ALL || action ==InventoryAction.PLACE_ONE || action == InventoryAction.SWAP_WITH_CURSOR) {
			if(event.getClickedInventory() != topInventory) return;
			else if ((topHolder instanceof BrewingStand) && event.getSlotType() != InventoryType.SlotType.CRAFTING) return;
			item = event.getCursor();
		} else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if(event.getClickedInventory() == topInventory)	return;
			item = event.getCurrentItem();
		} else return;
		
		if(item.getType() != Material.POTION && topHolder instanceof Barrel) {
			event.setCancelled(true);
			return;
		} else if (hasFilter(item)) {
			return;
		}
		//Brewery.breweryDriver.debugLog("Clear to proceed");
		NBTItem nbti = new NBTItem(item);
		if(nbti.hasKey("brewery")) {
			Brewery.breweryDriver.debugLog("Brew placed in Brewing Equipment");
			NBTCompound brewery = nbti.getCompound("brewery");
			
			//Ignore if already finished
			if(brewery.hasKey("finishedAging") && topHolder instanceof Barrel) {
				return;
			}
			
			if(!brewery.hasKey("placedInBrewer")) {
				//Cancel event
				event.setCancelled(true);
				
				brewery.setString("placedInBrewer", ((Player)event.getWhoClicked()).getUniqueId().toString());
				item = nbti.getItem();
				
				//Set the item, based on action used
				if(action == InventoryAction.PLACE_ALL || action ==InventoryAction.PLACE_ONE || action == InventoryAction.SWAP_WITH_CURSOR) {
					event.getWhoClicked().setItemOnCursor(action == InventoryAction.SWAP_WITH_CURSOR? event.getCurrentItem() : new ItemStack(Material.AIR));
					//event.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
					event.setCurrentItem(item);
				}  else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
					topInventory.addItem(item);
					event.setCurrentItem(new ItemStack(Material.AIR));
				}
				Brewery.breweryDriver.debugLog("Brew has been tagged");
			} 
		}
	}
	
	
	//Check if a brew is transferred into a brewing stand or barrel
		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onTransferFromBrewer(InventoryClickEvent event) {
			ItemStack item = null;
			String equipmentType = "none";
			
			//Check if we're dealing with a barrel or a brewing stand
			InventoryView invView = event.getView();
			Inventory topInventory = invView.getTopInventory();
			Inventory bottomInventory = invView.getBottomInventory();
			InventoryHolder topHolder = topInventory.getHolder();
			if(topHolder instanceof Barrel) {
				equipmentType = "Barrel";
			} else if(topHolder instanceof BrewingStand) {
				equipmentType = "BrewingStand";
			} else {
				//Brewery.breweryDriver.debugLog("Ignoring, neither barrel nor brewing stand.");
				//Brewery.breweryDriver.debugLog(topHolder.toString());
				return;//Not those two types, then ignore.
			}
			
			//Check action, leave if not the correct action to remove from a brewing equipment
			InventoryAction action = event.getAction();
			if(action == InventoryAction.PLACE_ALL || action ==InventoryAction.PLACE_ONE || action == InventoryAction.SWAP_WITH_CURSOR) {
				if(event.getClickedInventory() != bottomInventory) return;
				item = event.getCursor();
			} else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if(event.getClickedInventory() == bottomInventory)	return;
				item = event.getCurrentItem();
			} else return;

			
			//Brewery.breweryDriver.debugLog("Clear to proceed");
			NBTItem nbti = new NBTItem(item);
			if(nbti.hasKey("brewery")) {
				NBTCompound brewery = nbti.getCompound("brewery");
				Brewery.breweryDriver.debugLog("Transferred back");
				if(brewery.hasKey("placedInBrewer")) {
					Brewery.breweryDriver.debugLog("And it has the tag");
					if(item.getItemMeta().getDisplayName().contains("#Aging") || item.getItemMeta().getDisplayName().contains("#Distilling")) {
						event.setCancelled(true);
						item = BRecipe.revealMaskedBrew(item, equipmentType);
						//Set the item, based on action used
					} else {
						Brewery.breweryDriver.debugLog("Tag removed - no distillation happened");
						brewery.removeKey("placedInBrewer");
						item = nbti.getItem();
					}
					
					if(action == InventoryAction.PLACE_ALL || action ==InventoryAction.PLACE_ONE || action == InventoryAction.SWAP_WITH_CURSOR) {
						event.getWhoClicked().setItemOnCursor(action == InventoryAction.SWAP_WITH_CURSOR? event.getCurrentItem() : new ItemStack(Material.AIR));
						//event.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
						event.setCurrentItem(item);
					} else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
						bottomInventory.addItem(item);
						event.setCurrentItem(new ItemStack(Material.AIR));
					}
					((Player)event.getWhoClicked()).updateInventory();
				}
			}
		}***/
		
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
		if (!(event.getView().getTopInventory().getHolder() instanceof Barrel)) {
			return;
		}
		Brewery.breweryDriver.debugLog("Barrel click open");
		Barrel barrel = (Barrel) event.getView().getTopInventory().getHolder();
		if(barrel.isAging()) {
			if(event.isShiftClick() || (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Barrel)) {
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
		if (!(event.getView().getTopInventory().getHolder() instanceof Barrel)) {
			return;
		}
		Brewery.breweryDriver.debugLog("barrel drag open");
		Barrel barrel = (Barrel) event.getView().getTopInventory().getHolder();
		if(barrel.isAging()) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			Brewery.breweryDriver.debugLog("barrel cancelled interaction");
		}
	}

	// block the pickup of items where getPickupDelay is > 1000 (puke)
	@EventHandler(ignoreCancelled = true)
	public void onInventoryPickupItem(InventoryPickupItemEvent event){
		if (event.getItem().getPickupDelay() > 1000 && event.getItem().getItemStack().getType() == BPlayer.pukeItem) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getInventory().getHolder() instanceof Barrel) {
			event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
		}
	}
}
