package com.dreamless.brewery.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.BreweryCauldron;
import com.dreamless.brewery.entity.BreweryDistiller;
import com.dreamless.brewery.entity.BreweryDistiller.DistillerRunnable;
import com.dreamless.brewery.player.BPlayer;
import com.dreamless.brewery.player.Wakeup;
import com.dreamless.brewery.player.Words;
import com.dreamless.brewery.utils.BreweryMessage;

import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTTileEntity;

public class PlayerListener implements Listener {
	public static boolean openEverywhere;

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();

		if (clickedBlock != null) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player player = event.getPlayer();
				if (!player.isSneaking()) {
					Material type = clickedBlock.getType();

					// Interacting with a Cauldron
					if (type == Material.CAULDRON) {
						handleCauldron(event, player);
					} else if (type == Material.BREWING_STAND) {
						handleDistiller(event, player);
					} else if (type == Material.BARREL) {
						player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Section 1");
						handleBarrel(event, player);
					}
				}
			}
		}
	}

	private void handleCauldron(PlayerInteractEvent event, Player player) {
		Block clickedBlock = event.getClickedBlock();
		Material materialInHand = event.getMaterial();
		ItemStack item = event.getItem();

		if (materialInHand == null) {
			return;
		} else if (materialInHand == Material.BUCKET) {
			BreweryCauldron.remove(clickedBlock);
			return;
		} else if (materialInHand == Material.CLOCK) {
			if (BreweryCauldron.isCooking(clickedBlock)) {// Print time if cooking
				BreweryCauldron.printTime(player, clickedBlock);
			} else if (((Levelled) clickedBlock.getBlockData()).getLevel() > 0) {
				BreweryMessage result = BreweryCauldron.startCooking(clickedBlock, player);
				if (result.getResult()) {// Start cooking
					clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
							1.0f, 1.0f);
				}
				Brewery.breweryDriver.msg(player, result.getMessage());
			}
			return;
		} else if (materialInHand == Material.IRON_SHOVEL) {// Interact with inventory
			if (player.hasPermission("brewery.cauldron.insert")) {
				Inventory inventory = BreweryCauldron.getInventory(clickedBlock);
				if (inventory != null) {
					player.openInventory(inventory);
				}
			} else {
				Brewery.breweryDriver.msg(player, Brewery.getText("Perms_NoCauldronInsert"));
			}
			// player.openInventory(BCauldron.getInventory(clickedBlock));
			return;
		} else if (materialInHand == Material.GLASS_BOTTLE) { // fill a glass bottle with potion
			if (BreweryCauldron.isCooking(clickedBlock) && player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
				if (BreweryCauldron.fill(player, clickedBlock)) {
					event.setCancelled(true);
					if (player.hasPermission("brewery.cauldron.fill")) {
						if (item.getAmount() > 1) {
							item.setAmount(item.getAmount() - 1);
						} else {
							setItemInHand(event, Material.AIR, false);
						}
					}
				}
			} else {
				event.setCancelled(true);
			}
		} else if (materialInHand == Material.WATER_BUCKET) { // reset cauldron when refilling to prevent unlimited
																// source of potions
			if (BreweryCauldron.getFillLevel(clickedBlock) != 0 && BreweryCauldron.getFillLevel(clickedBlock) < 2) {
				// will only remove when existing
				BreweryCauldron.remove(clickedBlock);
			}
		}
	}

	private void handleBarrel(PlayerInteractEvent event, Player player) {
		player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Section 2");
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Section 3");
		Block block = event.getClickedBlock();
		NBTTileEntity entity = new NBTTileEntity(block.getState());
		if (entity.hasKey("TestString")) {
			player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + entity.getString("TestString"));
		} else {
			player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Section 4");
		}
		// Get the barrel
		BreweryBarrel barrel = BreweryBarrel.getBarrel(event.getClickedBlock());
		if (barrel != null) {
			event.setCancelled(true);
			if (!barrel.hasPermsOpen(player, event)) {
				return;
			}
		} else {
			return;
		}

		if (event.getMaterial() == Material.CLOCK && !barrel.isAging()) {
			BreweryMessage result = barrel.startAging(player);
			if (result.getResult()) {// Start cooking
				event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f,
						1.0f);
			}
			Brewery.breweryDriver.msg(player, result.getMessage());
		}
	}

	private void handleDistiller(PlayerInteractEvent event, Player player) {
		Block clickedBlock = event.getClickedBlock();
		Material materialInHand = event.getMaterial();

		BreweryDistiller distiller = BreweryDistiller.get(clickedBlock);

		// Cancel interaction if distilling
		if (distiller != null && distiller.isDistilling()) {
			event.setCancelled(true);
			return;
		}

		if (materialInHand == Material.IRON_SHOVEL) {
			// TODO: Brewer's tool
			event.setCancelled(true);
			if (distiller == null) {// Add a new one
				distiller = new BreweryDistiller(clickedBlock);
			}

			player.openInventory(distiller.getInventory());
		} else if (materialInHand == Material.CLOCK) {
			event.setCancelled(true);
			if (distiller != null) {
				BreweryMessage breweryMessage = distiller.startDistilling(player);
				Brewery.breweryDriver.msg(player, breweryMessage.getMessage());
				if (breweryMessage.getResult()) {
					clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 2.0f, 1.0f);
					BreweryDistiller.runningDistillers.put(distiller,
							new DistillerRunnable(BreweryDistiller.DEFAULT_CYCLE_LENGTH, distiller)
									.runTaskTimer(Brewery.breweryDriver, 20, 20).getTaskId());
				}
			}
		}
	}

	public void setItemInHand(PlayerInteractEvent event, Material mat, boolean swapped) {
		if ((event.getHand() == EquipmentSlot.OFF_HAND) != swapped) {
			event.getPlayer().getInventory().setItemInOffHand(new ItemStack(mat));
		} else {
			event.getPlayer().getInventory().setItemInMainHand(new ItemStack(mat));
		}
	}

	@EventHandler
	public void onClickAir(PlayerInteractEvent event) {
		if (Wakeup.checkPlayer == null)
			return;

		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (!event.hasItem()) {
				if (event.getPlayer() == Wakeup.checkPlayer) {
					Wakeup.tpNext();
				}
			}
		}
	}

	// player drinks a custom potion
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				NBTItem nbti = new NBTItem(item);
				if (nbti.hasKey("brewery")) {
					BPlayer.drink(player, item);
				}
			} else if (BPlayer.drainItems.containsKey(item.getType())) {
				BPlayer bplayer = BPlayer.get(player);
				if (bplayer != null) {
					bplayer.drainByItem(player, item.getType());
				}
			}
		}
	}

	// Player has died! Decrease Drunkeness by 20
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		BPlayer bPlayer = BPlayer.get(event.getPlayer());
		if (bPlayer != null) {
			if (bPlayer.getDrunkeness() > 20) {
				bPlayer.setData(bPlayer.getDrunkeness() - 20);
			} else {
				BPlayer.remove(event.getPlayer());
			}
		}
	}

	// player walks while drunk, push him around!
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (BPlayer.hasPlayer(event.getPlayer()) && BPlayer.get(event.getPlayer()).isDrunkEffects()) {
			BPlayer.playerMove(event);
		}
	}

	// player talks while drunk, but he cant speak very well
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Words.playerChat(event);
	}

	// player commands while drunk, distort chat commands
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
		Words.playerCommand(event);
	}

	// player joins while passed out
	@EventHandler()
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
			final Player player = event.getPlayer();
			BPlayer bplayer = BPlayer.get(player);
			if (bplayer != null) {
				if (player.hasPermission("brewery.bypass.logindeny")) {
					if (bplayer.getDrunkeness() > 100) {
						bplayer.setData(100);
					}
					bplayer.join(player);
					return;
				}
				switch (bplayer.canJoin()) {
				case 0:
					bplayer.join(player);
					return;
				case 2:
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Brewery.getText("Player_LoginDeny"));
					return;
				case 3:
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Brewery.getText("Player_LoginDenyLong"));
				}
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}
}
