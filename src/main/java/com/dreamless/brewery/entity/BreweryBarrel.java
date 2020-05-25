package com.dreamless.brewery.entity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.BarrelType;
import com.dreamless.brewery.brew.BrewItemFactory;
import com.dreamless.brewery.utils.BreweryMessage;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BreweryBarrel {

	public static HashSet<BreweryBarrel> barrels = new HashSet<BreweryBarrel>();

	// Difficulty adjustments
	public static double minutesPerYear = 20.0;

	private Barrel barrel;
	private int time;
	private Hologram hologram;
	//private boolean aging = false;
	private BarrelType type;

	public BreweryBarrel(Barrel block, BarrelType barrelType, int time) {
		this.barrel = block;
		this.time = time;
		//this.aging = aging;
		this.type = barrelType;

		barrels.add(this);

		createHologram(block.getBlock());
		updateHologram();
	}

	private void createHologram(Block block) {
		Location above = block.getRelative(BlockFace.UP).getLocation();
		above.setX(above.getX() + 0.5);
		above.setY(above.getY() + 0.75);
		above.setZ(above.getZ() + 0.5);
		hologram = HologramsAPI.createHologram(Brewery.breweryDriver, above);
	}

	private void updateHologram() {
		hologram.clearLines();
		hologram.appendTextLine(type.toString() + " barrel");
	    hologram.appendTextLine("Aged " + (int) time + " years"); // TODO: Fix
	}

	public boolean hasPermsOpen(Player player, PlayerInteractEvent event) {
		if (!player.hasPermission("brewery.openbarrel.big")) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Error_NoBarrelAccess"));
			return false;
		}
		return true;
	}

	// removes a barrel, throwing included potions to the ground
	public void remove(Block broken, Player breaker) {
			for (HumanEntity human : barrel.getInventory().getViewers()) {
				human.closeInventory();
			}
			ItemStack[] items = barrel.getInventory().getContents();
			barrel.getInventory().clear();
			
			// Finish Aging
			for (int i = 0; i < items.length; i++) {
				ItemStack item = items[i];
				// Brewery.breweryDriver.debugLog("Pull item");
				if (item == null) {
					continue;
				}
				items[i] = BrewItemFactory.getAgedBrew(item, time, type);
			}

			// Drop items
			Location dropLocation = barrel.getBlock().getRelative(((Directional)barrel.getBlockData()).getFacing()).getLocation().add(0.5, 0.5, 0.5);
			for (ItemStack item : items) {
				if (item != null) {
						barrel.getWorld().dropItemNaturally(dropLocation, item);
						barrel.getWorld().playSound(dropLocation, Sound.ENTITY_ITEM_PICKUP,
								(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
				}
			}
		hologram.delete();
		barrels.remove(this);
	}
	
	//////////////////////////////////////////////////////////////////////////
	public static BreweryBarrel getBarrel(Block block) {
		return null;	
	}
	
	// unloads barrels that are in a unloading world
	public static void onUnload(String name) {
		for (BreweryBarrel barrel : barrels) {
			if (barrel.barrel.getWorld().getName().equals(name)) {
				barrels.remove(barrel);
			}
		}
	}

	public static void onUpdate() {
		// Brewery.breweryDriver.debugLog("Update Barrel");
		for (BreweryBarrel barrel : barrels) {
			++barrel.time;
			barrel.updateHologram();
		}
	}

	// Saves all data
	public static void save() {
		int id = 0;
		if (!barrels.isEmpty()) {
	
			for (BreweryBarrel barrel : barrels) {
				Brewery.breweryDriver.debugLog("BARREL");
				// BlockData
				String location = Brewery.gson.toJson(barrel.barrel.getLocation().serialize());
				Brewery.breweryDriver.debugLog(location);
	
				// aging
	
				String query = "REPLACE " + Brewery.getDatabase("barrels")
						+ "barrels SET idbarrels=?, location=?, type=?, time=?";
				try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
					stmt.setInt(1, id);
					stmt.setString(2, location);
					stmt.setString(3, barrel.type.name());
					stmt.setFloat(3, barrel.time);
	
					Brewery.breweryDriver.debugLog(stmt.toString());
	
					stmt.executeUpdate();
				} catch (SQLException e1) {
					e1.printStackTrace();
					return;
				}
				id++;
			}
		}
		// clean up extras
		String query = "DELETE FROM " + Brewery.getDatabase("barrels") + "barrels WHERE idbarrels >=?";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setInt(1, id);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
}
