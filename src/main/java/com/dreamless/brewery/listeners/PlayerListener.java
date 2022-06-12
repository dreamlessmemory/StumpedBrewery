package com.dreamless.brewery.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.BarrelType;
import com.dreamless.brewery.brew.MashBucket;
import com.dreamless.brewery.brew.Rarity;
import com.dreamless.brewery.data.NBTConstants;
import com.dreamless.brewery.entity.BreweryBarrel;
import com.dreamless.brewery.entity.BreweryCauldron;
import com.dreamless.brewery.entity.BreweryDistiller;
import com.dreamless.brewery.entity.BreweryMashBarrel;
import com.dreamless.brewery.item.BarrelLidItem;
import com.dreamless.brewery.player.BPlayer;
import com.dreamless.brewery.player.Wakeup;
import com.dreamless.brewery.player.Words;
import com.dreamless.brewery.utils.BreweryMessage;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTItem;

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

					// event.setCancelled(true);
					
					if(!checkCreative(player))
					{
						return;
					}
					
					switch(type)
					{
					case CAULDRON:
						// TODO Implement
						//handleCauldron(event, player);
						break;
					case WATER_CAULDRON:
						handleWaterCauldron(event, player);
						break;
					case BARREL:
						handleBarrel(event, player);
						break;
					default:
						if (MashBucket.isMashBucket(event.getItem()))
						{
							MashBucket.dumpContents(event.getItem(), event.getClickedBlock().getLocation());
						}
						break;
					}
				}
				else
				{
					if (MashBucket.isMashBucket(event.getItem()))
					{
						MashBucket.dumpContents(event.getItem(), event.getClickedBlock().getLocation());
					}
				}
			}
		}
	}

	private void handleCauldron(PlayerInteractEvent event, Player player) {
		Block clickedBlock = event.getClickedBlock();
		BreweryCauldron cauldron = BreweryCauldron.get(clickedBlock);
		Material materialInHand = event.getMaterial();
		ItemStack item = event.getItem();

		if (cauldron == null && BreweryCauldron.isUseableCauldron(clickedBlock)
				&& Rarity.isValidIngredient(materialInHand)) {

			event.setCancelled(true);

			cauldron = new BreweryCauldron(clickedBlock);
			cauldron.addIngredient(materialInHand);
			removeItemFromPlayerHand(player);
			return;
		} else if (cauldron != null) {
			if (materialInHand == Material.BUCKET) {
				event.setCancelled(true);
				BreweryCauldron.remove(cauldron);
			} else if (materialInHand == Material.CLOCK) {
				event.setCancelled(true);
				if (cauldron.isCooking()) {// Print time if cooking
					cauldron.printTime(player);
				} else if (cauldron.getFillLevel() > 0) {
					BreweryMessage result = cauldron.startCooking();
					if (result.getResult()) {// Start cooking
						clickedBlock.getWorld().playSound(clickedBlock.getLocation(),
								Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f);
					}
					Brewery.breweryDriver.msg(player, result.getMessage());
				}
			} else if (materialInHand == Material.WOODEN_SHOVEL || materialInHand == Material.IRON_SHOVEL
					|| materialInHand == Material.STONE_SHOVEL || materialInHand == Material.GOLDEN_SHOVEL
					|| materialInHand == Material.DIAMOND_SHOVEL) {// Purge
				event.setCancelled(true);
				if (player.hasPermission("brewery.cauldron.insert") && !cauldron.isCooking()) {
					BreweryCauldron.remove(cauldron);
				} else {
					Brewery.breweryDriver.msg(player, Brewery.getText("Perms_NoCauldronInsert"));
				}
			} else if (materialInHand == Material.GLASS_BOTTLE) { // fill a glass bottle with potion
				if (cauldron.isCooking() && player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
					if (BreweryCauldron.fillBottle(player, clickedBlock)) {
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
				if (cauldron.getFillLevel() != 0 && cauldron.getFillLevel() < 2) {
					// will only remove when existing
					BreweryCauldron.remove(cauldron);
				}
			} else if (Rarity.isValidIngredient(materialInHand)) { // Add ingredient
				event.setCancelled(true);
				BreweryMessage response = cauldron.addIngredient(materialInHand);
				if (response.getResult()) {
					removeItemFromPlayerHand(player);
				} else {
					Brewery.breweryDriver.msg(player, response.getMessage());
				}
			}
		}
	}
	
	private void handleWaterCauldron(PlayerInteractEvent event, Player player) {
		Block clickedBlock = event.getClickedBlock();
		BreweryCauldron cauldron = BreweryCauldron.get(clickedBlock);
		Material materialInHand = event.getMaterial();
		ItemStack item = event.getItem();

		if (cauldron == null && BreweryCauldron.isUseableCauldron(clickedBlock)
				&& Rarity.isValidIngredient(materialInHand)) {

			event.setCancelled(true);

			cauldron = new BreweryCauldron(clickedBlock);
			cauldron.addIngredient(materialInHand);
			removeItemFromPlayerHand(player);
			return;
		} else if (cauldron != null) {
			if (materialInHand == Material.BUCKET) {
				event.setCancelled(true);
				BreweryCauldron.remove(cauldron);
			} else if (materialInHand == Material.CLOCK) {
				event.setCancelled(true);
				if (cauldron.isCooking()) {// Print time if cooking
					cauldron.printTime(player);
				} else if (cauldron.getFillLevel() > 0) {
					BreweryMessage result = cauldron.startCooking();
					if (result.getResult()) {// Start cooking
						clickedBlock.getWorld().playSound(clickedBlock.getLocation(),
								Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f);
					}
					Brewery.breweryDriver.msg(player, result.getMessage());
				}
			} else if (materialInHand == Material.WOODEN_SHOVEL || materialInHand == Material.IRON_SHOVEL
					|| materialInHand == Material.STONE_SHOVEL || materialInHand == Material.GOLDEN_SHOVEL
					|| materialInHand == Material.DIAMOND_SHOVEL) {// Purge
				event.setCancelled(true);
				if (player.hasPermission("brewery.cauldron.insert") && !cauldron.isCooking()) {
					BreweryCauldron.remove(cauldron);
				} else {
					Brewery.breweryDriver.msg(player, Brewery.getText("Perms_NoCauldronInsert"));
				}
			} else if (materialInHand == Material.GLASS_BOTTLE) { // fill a glass bottle with potion
				if (cauldron.isCooking() && player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
					if (BreweryCauldron.fillBottle(player, clickedBlock)) {
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
				if (cauldron.getFillLevel() != 0 && cauldron.getFillLevel() < 2) {
					// will only remove when existing
					BreweryCauldron.remove(cauldron);
				}
			} else if (Rarity.isValidIngredient(materialInHand)) { // Add ingredient
				event.setCancelled(true);
				BreweryMessage response = cauldron.addIngredient(materialInHand);
				if (response.getResult()) {
					removeItemFromPlayerHand(player);
				} else {
					Brewery.breweryDriver.msg(player, response.getMessage());
				}
			}
		}
	}

	private void handleBarrel(PlayerInteractEvent event, Player player) {
		// Get the barrel
		BreweryBarrel barrel = BreweryBarrel.getBarrel(event.getClickedBlock());

		// If actually a barrel open if it's an ax
		if (barrel != null) {
			event.setCancelled(true);
			if (isAxe(event.getItem())) {
				barrel.removeAndFinishBrewing(event.getClickedBlock(), player);
			}
		} else {
			if(event.getItem() == null)
			{
				// Just exit
				return;
			}
			if(BreweryBarrel.isBarrelLid(event.getItem()))
			{
				BarrelType type = BarrelLidItem.getBarrelType(event.getItem());
				if (type != null) {
					event.setCancelled(true);
					barrel = new BreweryBarrel((Barrel) event.getClickedBlock().getState(), type, 0);
					event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f,
							1.0f);
					Brewery.breweryDriver.msg(player, Brewery.getText("Barrel_Start_Aging"));
					removeItemFromPlayerHand(player);
				}
			}
			else if (BreweryUtils.isAxe(event.getItem()))
			{
				event.setCancelled(true);
				BreweryMashBarrel.getMashBucket(event.getClickedBlock(), player);
			}
		}
	}

	/*******************************
	 * HELPER METHODS
	 *******************************/

	private void setItemInHand(PlayerInteractEvent event, Material mat, boolean swapped) {
		if ((event.getHand() == EquipmentSlot.OFF_HAND) != swapped) {
			event.getPlayer().getInventory().setItemInOffHand(new ItemStack(mat));
		} else {
			event.getPlayer().getInventory().setItemInMainHand(new ItemStack(mat));
		}
	}

	/*******************************
	 * PLAYER/DRUNKENESS INTERACTIONS
	 *******************************/

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
				if (nbti.hasKey(NBTConstants.BREWERY_TAG_STRING)) {
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

	private void removeItemFromPlayerHand(Player player) {
		ItemStack itemInHand = player.getInventory().getItemInMainHand();
		if (itemInHand.getAmount() == 1) {
			player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
		} else {
			itemInHand.setAmount(itemInHand.getAmount() - 1);
			player.getInventory().setItemInMainHand(itemInHand);
		}
	}

	private boolean checkCreative(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE && !Brewery.permitcreative) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Player_CreativeNotAllowed"));
			return false;
		} else {
			return true;
		}
	}

	private boolean isAxe(ItemStack item) {
		if (item == null) {
			return false;
		}
		switch (item.getType()) {
		case WOODEN_AXE:
		case STONE_AXE:
		case IRON_AXE:
		case GOLDEN_AXE:
		case DIAMOND_AXE:
			return true;
		default:
			return false;
		}
	}
}
