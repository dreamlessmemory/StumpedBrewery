package com.dreamless.brewery.player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.data.NBTConstants;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BPlayer {
	private static Map<String, BPlayer> players = new HashMap<String, BPlayer>();// Players name/uuid and BPlayer
	private static Map<Player, MutableInt> pTasks = new HashMap<Player, MutableInt>();// Player and count
	private static int taskId;
	private static boolean modAge = true;
	private static Random pukeRand;
	private static Method gh;
	private static Field age;

	// Settings
	public static Map<Material, Integer> drainItems = new HashMap<Material, Integer>();// DrainItem Material and Strength
	public static Material pukeItem;
	public static int pukeDespawntime;
	public static int hangoverTime;
	public static boolean overdrinkKick;
	public static boolean enableHome;
	public static boolean enableLoginDisallow;
	public static boolean enablePuke;
	public static String homeType;

	//private int quality = 0;// = quality of drunkeness * drunkeness
	private int drunkeness = 0;// = amount of drunkeness
	private int offlineDrunk = 0;// drunkeness when gone offline
	private Vector push = new Vector(0, 0, 0);
	private int time = 20;
	private boolean drunkEffects = false;

	public BPlayer() {
	}

	// reading from file
	public BPlayer(String name, int drunkeness, int offlineDrunk, boolean drunkEffects) {
		this.drunkeness = drunkeness;
		this.offlineDrunk = offlineDrunk;
		this.drunkEffects = drunkEffects;
		players.put(name, this);
	}

	public static BPlayer get(Player player) {
		if (!players.isEmpty()) {
			return players.get(Brewery.playerString(player));
		}
		return null;
	}

	// This method may be slow and should not be used if not needed
	public static BPlayer getByName(String playerName) {
		if (Brewery.useUUID) {
			for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
				OfflinePlayer p = Brewery.breweryDriver.getServer().getOfflinePlayer(UUID.fromString(entry.getKey()));
				if (p != null) {
					String name = p.getName();
					if (name != null) {
						if (name.equalsIgnoreCase(playerName)) {
							return entry.getValue();
						}
					}
				}
			}
			return null;
		}
		return players.get(playerName);
	}

	// This method may be slow and should not be used if not needed
	public static boolean hasPlayerbyName(String playerName) {
		if (Brewery.useUUID) {
			for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
				OfflinePlayer p = Brewery.breweryDriver.getServer().getOfflinePlayer(UUID.fromString(entry.getKey()));
				if (p != null) {
					String name = p.getName();
					if (name != null) {
						if (name.equalsIgnoreCase(playerName)) {
							return true;
						}
					}
				}
			}
			return false;
		}
		return players.containsKey(playerName);
	}

	public static boolean isEmpty() {
		return players.isEmpty();
	}

	public static boolean hasPlayer(Player player) {
		return players.containsKey(Brewery.playerString(player));
	}

	// Create a new BPlayer and add it to the list
	public static BPlayer addPlayer(Player player) {
		BPlayer bPlayer = new BPlayer();
		players.put(player.getUniqueId().toString(), bPlayer);
		return bPlayer;
	}

	public static void remove(Player player) {
		players.remove(Brewery.playerString(player));
		//SQL
		String query = "INSERT INTO " + Brewery.getDatabase("players") + "players (uuid, quality, drunkeness, offlinedrunk) VALUES (?, 0, 0, 0) ON DUPLICATE KEY UPDATE quality=0, drunkeness=0, offlinedrunk=0";
		try(PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, player.getUniqueId().toString());
			//Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void remove() {
		for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
			if (entry.getValue() == this) {
				players.remove(entry.getKey());
				
				//SQL
				String query = "INSERT INTO " + Brewery.getDatabase("players") + "players (uuid, quality, drunkeness, offlinedrunk) VALUES (?, 0, 0, 0) ON DUPLICATE KEY UPDATE quality=0, drunkeness=0, offlinedrunk=0";
				try(PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
					stmt.setString(1, entry.getKey());
					Brewery.breweryDriver.debugLog(stmt.toString());
					stmt.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return;
			}
		}
	}

	public static void clear() {
		players.clear();
	}

	// Drink a brew and apply effects, etc.
	public static void drink(Player player, ItemStack item) {
		//Get if player wants to be drunk.
		String query = "SELECT * FROM " + Brewery.getDatabase("players") + "players WHERE uuid=?";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, player.getUniqueId().toString());
			//Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results = stmt.executeQuery();
			if(!results.next()) {
				return; //Get out, by default people don't want to be drunk
			} else {
				if(!results.getBoolean("drunkeffects")) {
					return;//Player does not want to be drunk
				}
				//Player Management
				BPlayer bPlayer = get(player);
				if (bPlayer == null) {
					bPlayer = addPlayer(player);
				}
				
				NBTItem nbti = new NBTItem(item);
				NBTCompound brewery = nbti.getCompound(NBTConstants.BREWERY_TAG_STRING);	
				bPlayer.drunkeness += Math.min(30, brewery.getInteger(NBTConstants.EFFECT_SCORE_TAG_STRING) * 3);
				bPlayer.drunkEffects = true;
				
				if(bPlayer.drunkeness > 100) {
					bPlayer.drinkCap(player);
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	// Player has drunken too much
	public void drinkCap(Player player) {
		//quality = getQuality() * 100;
		drunkeness = 100;
		if (overdrinkKick && !player.hasPermission("brewery.bypass.overdrink")) {
			passOut(player);
		} else {
			addPuke(player, 60 + (int) (Math.random() * 60.0));
			Brewery.breweryDriver.msg(player, Brewery.getText("Player_CantDrink"));
		}
	}

	// push the player around if he moves
	public static void playerMove(PlayerMoveEvent event) {
		BPlayer bPlayer = get(event.getPlayer());
		if (bPlayer != null) {
			bPlayer.move(event);
		}
	}

	// Eat something to drain the drunkeness
	public void drainByItem(Player player, Material mat) {
		int strength = drainItems.get(mat);
		if (drain(player, strength)) {
			remove(player);
		}
	}

	// drain the drunkeness by amount, returns true when player has to be removed
	public boolean drain(Player player, int amount) {
		drunkeness -= amount;
		if (drunkeness > 0) {
			if (offlineDrunk == 0) {
				if (player == null) {
					offlineDrunk = drunkeness;
				}
			}
		} else {
			if (offlineDrunk == 0) {
				return true;
			}
			if (drunkeness <= -offlineDrunk) {
				if (drunkeness <= -hangoverTime) {
					return true;
				}
			}
		}
		return false;
	}

	// player is drunk
	public void move(PlayerMoveEvent event) {
		// has player more alc than 10
		if (drunkeness >= 10) {
			if (drunkeness <= 100) {
				if (time > 1) {
					time--;
				} else {
					// Is he moving
					if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
						Player player = event.getPlayer();
						Entity entity = (Entity) player;
						// not in midair
						if (entity.isOnGround()) {
							time--;
							if (time == 0) {
								// push him only to the side? or any direction
								// like now
								push.setX((Math.random() - 0.5) / 2.0);
								push.setZ((Math.random() - 0.5) / 2.0);
								player.setVelocity(push);
							} else if (time < 0 && time > -10) {
								// push him some more in the same direction
								player.setVelocity(push);
							} else {
								// when more alc, push him more often
								time = (int) (Math.random() * (201.0 - (drunkeness * 2)));
							}
						}
					}
				}
			}
		}
	}

	public void passOut(Player player) {
		player.kickPlayer(Brewery.getText("Player_DrunkPassOut"));
		offlineDrunk = drunkeness;
	}


	// #### Login ####

	// can the player login or is he too drunk
	public int canJoin() {
		if (drunkeness <= 70) {
			return 0;
		}
		if (!enableLoginDisallow) {
			if (drunkeness <= 100) {
				return 0;
			} else {
				return 3;
			}
		}
		if (drunkeness <= 90) {
			if (Math.random() > 0.4) {
				return 0;
			} else {
				return 2;
			}
		}
		if (drunkeness <= 100) {
			if (Math.random() > 0.6) {
				return 0;
			} else {
				return 2;
			}
		}
		return 3;
	}

	// player joins
	public void join(final Player player) {
		if (offlineDrunk == 0) {
			return;
		}
		// delayed login event as the player is not fully accessible pre login
		Brewery.breweryDriver.getServer().getScheduler().runTaskLater(Brewery.breweryDriver, new Runnable() {
			public void run() {
				login(player);
			}
		}, 1L);
	}

	// he may be having a hangover
	public void login(final Player player) {
		if (drunkeness < 10) {
			if (offlineDrunk > 60) {
				if (enableHome && !player.hasPermission("brewery.bypass.teleport")) {
					goHome(player);
				}
			}
			hangoverEffects(player);
			// wird der spieler noch gebraucht?
			players.remove(Brewery.playerString(player));

		} else if (offlineDrunk - drunkeness >= 30) {
			Location randomLoc = Wakeup.getRandom(player.getLocation());
			if (randomLoc != null) {
				if (!player.hasPermission("brewery.bypass.teleport")) {
					player.teleport(randomLoc);
					Brewery.breweryDriver.msg(player, Brewery.getText("Player_Wake"));
				}
			}
		}

		offlineDrunk = 0;
	}

	public void disconnecting() {
		offlineDrunk = drunkeness;
	}

	public void goHome(final Player player) {
		if (homeType != null) {
			Location home = null;
			if (homeType.equalsIgnoreCase("bed")) {
				home = player.getBedSpawnLocation();
			} else if (homeType.startsWith("cmd: ")) {
				player.performCommand(homeType.substring(5));
			} else if (homeType.startsWith("cmd:")) {
				player.performCommand(homeType.substring(4));
			} else {
				Brewery.breweryDriver.errorLog("Config.yml 'homeType: " + homeType + "' unknown!");
			}
			if (home != null) {
				player.teleport(home);
			}
		}
	}


	// #### Puking ####

	// Chance that players puke on big drunkeness
	// runs every 6 sec, average chance is 10%, so should puke about every 60 sec
	// good quality can decrease the chance by up to 10%
	public void drunkPuke(Player player) {
		if (drunkeness >= 80) {
			if (drunkeness >= 90) {
				if (Math.random() < 0.15 - (drunkeness / 100)) {
					addPuke(player, 20 + (int) (Math.random() * 40));
				}
			} else {
				if (Math.random() < 0.08 - (drunkeness / 100)) {
					addPuke(player, 10 + (int) (Math.random() * 30));
				}
			}
		}
	}

	// make a Player puke "count" items
	public static void addPuke(Player player, int count) {
		if (!enablePuke) {
			return;
		}

		if (pTasks.isEmpty()) {
			taskId = Brewery.breweryDriver.getServer().getScheduler().scheduleSyncRepeatingTask(Brewery.breweryDriver, new Runnable() {
				public void run() {
					pukeTask();
				}
			}, 1L, 1L);
		}
		pTasks.put(player, new MutableInt(count));
	}

	public static void pukeTask() {
		for (Iterator<Map.Entry<Player, MutableInt>> iter = pTasks.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<Player, MutableInt> entry = iter.next();
			Player player = entry.getKey();
			MutableInt count = entry.getValue();
			if (!player.isValid() || !player.isOnline()) {
				iter.remove();
			}
			puke(player);
			count.decrement();
			if (count.intValue() <= 0) {
				iter.remove();
			}
		}
		if (pTasks.isEmpty()) {
			Brewery.breweryDriver.getServer().getScheduler().cancelTask(taskId);
		}
	}

	public static void puke(Player player) {
		if (pukeRand == null) {
			pukeRand = new Random();
		}
		if (pukeItem == null || pukeItem == Material.AIR) {
			pukeItem = Material.SOUL_SAND;
		}
		Location loc = player.getLocation();
		loc.setY(loc.getY() + 1.1);
		loc.setPitch(loc.getPitch() - 10 + pukeRand.nextInt(20));
		loc.setYaw(loc.getYaw() - 10 + pukeRand.nextInt(20));
		Vector direction = loc.getDirection();
		direction.multiply(0.5);
		loc.add(direction);
		Item item = player.getWorld().dropItem(loc, new ItemStack(pukeItem));
		item.setVelocity(direction);
		item.setPickupDelay(32767); // Item can never be picked up when pickup delay is 32767
		//item.setTicksLived(6000 - pukeDespawntime); // Well this does not work...
		if (modAge) {
			if (pukeDespawntime >= 5800) {
				return;
			}
			try {
				if (gh == null) {
					gh = Class.forName(Brewery.breweryDriver.getServer().getClass().getPackage().getName() + ".entity.CraftItem").getMethod("getHandle", (Class<?>[]) null);
				}
				Object entityItem = gh.invoke(item, (Object[]) null);
				if (age == null) {
					age = entityItem.getClass().getDeclaredField("age");
					age.setAccessible(true);
				}

				// Setting the age determines when an item is despawned. At age 6000 it is removed.
				if (pukeDespawntime <= 0) {
					// Just show the item for a tick
					age.setInt(entityItem, 5999);
				} else if (pukeDespawntime <= 120) {
					// it should despawn in less than 6 sec. Add up to half of that randomly
					age.setInt(entityItem, 6000 - pukeDespawntime + pukeRand.nextInt((int) (pukeDespawntime / 2F)));
				} else {
					// Add up to 5 sec randomly
					age.setInt(entityItem, 6000 - pukeDespawntime + pukeRand.nextInt(100));
				}
				return;
			} catch (InvocationTargetException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
				e.printStackTrace();
			}
			modAge = false;
			Brewery.breweryDriver.errorLog("Failed to set Despawn Time on item " + pukeItem.name());
		}
	}


	// #### Effects ####

	public void drunkEffects(Player player) {
		int duration = 10;
		duration += drunkeness / 2;
		duration *= 20;
		if (duration > 960) {
			duration *= 5;
		} else if (duration < 460) {
			duration = 460;
		}
		PotionEffectType.CONFUSION.createEffect(duration, 0).apply(player);
	}

	public static void addQualityEffects(int quality, int brewAlc, Player player) {
		int duration = 7 - quality;
		if (quality == 0) {
			duration *= 500;
		} else if (quality <= 5) {
			duration *= 250;
		} else {
			duration = 100;
			if (brewAlc <= 10) {
				duration = 0;
			}
		}
		if (duration > 0) {
			PotionEffectType.POISON.createEffect(duration, 0).apply(player);
		}

		if (brewAlc > 10) {
			if (quality <= 5) {
				duration = 10 - quality;
				duration += brewAlc;
				duration *= 60;
			} else {
				duration = 120;
			}
			PotionEffectType.BLINDNESS.createEffect(duration, 0).apply(player);
		}
	}

	public void hangoverEffects(final Player player) {
		int duration = offlineDrunk * 1000;
		int amplifier = offlineDrunk / 30;

		PotionEffectType.SLOW.createEffect(duration, amplifier).apply(player);
		PotionEffectType.HUNGER.createEffect(duration, amplifier).apply(player);
	}


	// #### Sheduled ####

	public static void drunkeness() {
		for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
			String name = entry.getKey();
			BPlayer bplayer = entry.getValue();

			if (bplayer.drunkeness > 30 && bplayer.offlineDrunk == 0) {
				Player player = Bukkit.getPlayer(UUID.fromString(name));
				if (player != null && bplayer.isDrunkEffects()) {
					bplayer.drunkEffects(player);
					if (enablePuke) {
						bplayer.drunkPuke(player);
					}
				}
			}
		}
	}

	// decreasing drunkeness over time
	public static void onUpdate() {
		if (!players.isEmpty()) {
			int soberPerMin = 2;
			Iterator<Map.Entry<String, BPlayer>> iter = players.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, BPlayer> entry = iter.next();
				String name = entry.getKey();
				BPlayer bplayer = entry.getValue();
				if (bplayer.drunkeness == soberPerMin) {
					// Prevent 0 drunkeness
					soberPerMin++;
				}
				if (bplayer.drain(Bukkit.getPlayer(UUID.fromString(name)), soberPerMin)) {
					iter.remove();
				}
			}
		}
	}

	// save all data
	public static void save() {
		for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
			String query = "REPLACE " + Brewery.getDatabase("players") + "players SET uuid=?, drunkeness=?, offlinedrunk=?, drunkeffects=?";
			BPlayer bPlayer = entry.getValue();
			try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
				stmt.setString(1, entry.getKey());
				stmt.setInt(2, bPlayer.drunkeness);
				stmt.setInt(3, bPlayer.offlineDrunk);
				stmt.setBoolean(4, bPlayer.drunkEffects);
				//Brewery.breweryDriver.debugLog(stmt.toString());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}


	// #### getter/setter ####

	public int getDrunkeness() {
		return drunkeness;
	}

	public void setData(int drunkeness) {
		this.drunkeness = drunkeness;
	}

	public boolean isDrunkEffects() {
		return drunkEffects;
	}

	public void setDrunkEffects(boolean drunkEffects) {
		this.drunkEffects = drunkEffects;
	}

	public static void toggleDrunkEffects(boolean setDrunk, String uuid) {
		BPlayer bPlayer = players.get(uuid);
		if(bPlayer != null) {
			bPlayer.setDrunkEffects(setDrunk);
		}
		String query = "REPLACE " + Brewery.getDatabase("players") + "players SET drunkeffects=?, uuid=?";
		try(PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setBoolean(1, setDrunk);
			stmt.setString(2, uuid);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
