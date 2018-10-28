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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.dreamless.brewery.*;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;

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
		if (!Brewery.use1_9) return;
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

		Brewery.breweryDriver.debugLog("Starting brew inventory tracking");
		trackedBrewmen.add(player.getUniqueId());
	}

	/**
	 * Stop tracking distillation for a person when they close the brewer window.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClose(InventoryCloseEvent event) {
		if (!Brewery.use1_9) return;
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

		Brewery.breweryDriver.debugLog("Stopping brew inventory tracking");
		trackedBrewmen.remove(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerDrag(InventoryDragEvent event) {
		if (!Brewery.use1_9) return;
		// Workaround the Drag event when only clicking a slot
		if (event.getInventory() instanceof BrewerInventory) {
			onBrewerClick(new InventoryClickEvent(event.getView(), InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PLACE_ALL));
		}
	}

	/**
	 * Clicking can either start or stop the new brew distillation tracking.
	 * Note that server restart will halt any ongoing brewing processes and
	 * they will _not_ restart until a new click event.
	 *
	 * @param event the Click event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClick(InventoryClickEvent event) {
		if (!Brewery.use1_9) return;
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

	// Returns a Brew or null for every Slot in the BrewerInventory
	private Brew[] getDistillContents(BrewerInventory inv) {
		ItemStack item;
		Brew[] contents = new Brew[3];
		for (int slot = 0; slot < 3; slot++) {
			item = inv.getItem(slot);
			if (item != null) {
				contents[slot] = Brew.get(item);
			}
		}
		return contents;
	}

	private byte hasCustom(BrewerInventory brewer) {
		ItemStack item = brewer.getItem(3); // ingredient
		boolean glowstone = (item != null && Material.GLOWSTONE_DUST == item.getType()); // need dust in the top slot.
		byte customFound = 0;
		for (Brew brew : getDistillContents(brewer)) {
			if (brew != null) {
				if (!glowstone) {
					return 1;
				} else {
					customFound = 1;
				}
			}
		}
		return customFound;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBrew(BrewEvent event) {
		if (Brewery.use1_9) {
			if (hasCustom(event.getContents()) != 0) {
				event.setCancelled(true);
			}
			return;
		}
		if (runDistill(event.getContents())) {
			event.setCancelled(true);
		}
	}

	private boolean runDistill(BrewerInventory inv) {
		boolean custom = false;
		Brew[] contents = getDistillContents(inv);
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] == null) continue;
			custom = true;
		}
		if (custom) {
			Brew.distillAll(inv, contents);
			return true;
		}
		return false;
	}

	private int getLongestDistillTime(BrewerInventory inv) {
		return 800;
	}

	// Clicked a Brew somewhere, do some updating
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onInventoryClickLow(InventoryClickEvent event) {
		if (event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.POTION)) {
			ItemStack item = event.getCurrentItem();
			if (item.hasItemMeta()) {
				PotionMeta potion = ((PotionMeta) item.getItemMeta());
				Brew brew = Brew.get(potion);
				if (brew != null) {
					// convert potions from 1.8 to 1.9 for color and to remove effect descriptions
					if (Brewery.use1_9 && !potion.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) {
						BRecipe recipe = brew.getCurrentRecipe();
						if (recipe != null) {
							Brew.removeEffects(potion);
							//Brew.PotionColor.valueOf(recipe.getColor()).colorBrew(potion, item, brew.canDistill());
							item.setItemMeta(potion);
						}
					}
					brew.touch();
				}
			}
		}
	}
	//TODO: Convert to High
	//We're going to do the recipe check here.
	// convert to non colored Lore when taking out of Barrel/Brewer
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getType() == InventoryType.BREWING) {//Check if Brewing stand
			if (event.getSlot() > 2) {//Not a brewing stand, get out
				return;
			}
		} else if (!(event.getInventory().getHolder() instanceof Barrel)) {
			return; //Not a barrel, get out.
		}
		//TODO: Check recipe
		ItemStack item = event.getCurrentItem();
		if(item == null || item.getType() == Material.AIR) {
			Brewery.breweryDriver.debugLog("try get cursor");
			item = event.getCursor();
			if(item == null || item.getType() == Material.AIR) {
				Brewery.breweryDriver.debugLog("not the cursor either");
				return;
			}
		}
		//place all and place one do work
		NBTItem nbti = new NBTItem(item);
		if(nbti.hasKey("brewery")) {
			NBTCompound brewery = nbti.getCompound("brewery");
			boolean giveToPlayer = false;
			boolean getFromPlayer = false;
			
			switch(event.getAction()) {
				case PLACE_ALL:
				case PLACE_ONE:
				case PLACE_SOME:
					if(event.getClickedInventory().getHolder() instanceof Player) {
						Brewery.breweryDriver.debugLog("MOVE");
						giveToPlayer = true;
					} else {
						getFromPlayer = true;
					}
					break;
				case MOVE_TO_OTHER_INVENTORY:
					if(event.getClickedInventory().getHolder() instanceof Barrel || event.getClickedInventory().getHolder() instanceof BrewingStand) {
						Brewery.breweryDriver.debugLog("MOVE");
						giveToPlayer = true;
					} else {
						getFromPlayer = true;
					}
					break;
				default:
					break;
			}
			if(giveToPlayer) {
				//TODO: Remove Crafter
				//TODO: Reveal
				Brewery.breweryDriver.debugLog("really reveal?");
				
				if(brewery.hasKey("placedInBrewer")) {
					Brewery.breweryDriver.debugLog("ya reveal");
					event.setCurrentItem(Barrel.revealAgedBrew(item));
				}
			} else if (getFromPlayer) {
				//TODO:List as crafter
				if(!brewery.hasKey("placedInBrewer")) {
					brewery.setString("placedInBrewer", ((Player)event.getWhoClicked()).getUniqueId().toString());
					item = nbti.getItem();
					event.setCurrentItem(item);
					Brewery.breweryDriver.debugLog("Added crafter?");
				}
			}
		}
		//Brewery.breweryDriver.debugLog("TEST");
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
