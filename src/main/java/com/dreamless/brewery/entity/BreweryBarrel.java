package com.dreamless.brewery.entity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.BarrelType;
import com.dreamless.brewery.brew.BrewItemFactory;
import com.dreamless.brewery.brew.MashBucket;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

public class BreweryBarrel {

	public static HashSet<BreweryBarrel> barrels = new HashSet<BreweryBarrel>();

	// Difficulty adjustments
	public static double minutesPerYear = 20.0;

	// Constants
	private static final int MAX_BREWS_PER_BARREL = 27;

	private Barrel barrel;
	private int time;
	private Hologram hologram;
	private BarrelType type;
	private MashBucket mash;
	private int numberOfBrews;

	public BreweryBarrel(Barrel block, BarrelType barrelType, int time) {
		this.barrel = block;
		this.time = time;
		this.type = barrelType;

		barrels.add(this);
		if(Brewery.hologramsEnabled)
		{
			createHologram(block.getBlock());
			updateHologram();
		}
		
		ItemStack[] items = barrel.getInventory().getContents();
		mash = new MashBucket(items[0]);

		// Setup Drop list
		ArrayList<ItemStack> dropList = new ArrayList<ItemStack>();
		// Add Empty Bucket
		dropList.add(new ItemStack(Material.BUCKET));
		// Add Unneeded Bottles
		numberOfBrews = Math.min(mash.getNumberOfFillableBottles(), items[1].getAmount());
		if(numberOfBrews > MAX_BREWS_PER_BARREL)
		{
			numberOfBrews = MAX_BREWS_PER_BARREL;
		}
		if(numberOfBrews < items[1].getAmount())
		{
			dropList.add(new ItemStack((Material.GLASS_BOTTLE), items[1].getAmount() - numberOfBrews));
		}
		
		// Add other items		
		for (int i = 2; i < items.length; i++) {
			ItemStack item = items[i];
			if (item != null) {
				dropList.add(item);
			}
		}
		
		// Set Contents
		barrel.getInventory().clear();
		barrel.getInventory().setItem(0, new ItemStack(Material.GLASS_BOTTLE, numberOfBrews));

		// Drop
		Location dropLocation = barrel.getBlock().getRelative(((Directional)barrel.getBlockData()).getFacing()).getLocation().add(0.5, 0.5, 0.5);
		for (ItemStack dropItems : dropList) {
			barrel.getWorld().dropItemNaturally(dropLocation, dropItems);
			barrel.getWorld().playSound(dropLocation, Sound.ENTITY_ITEM_PICKUP,
					(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
		}
		
	}
	
	private void playAgingEffect()
	{
		barrel.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, barrel.getLocation().getX() + 0.5,
				barrel.getLocation().getY() + 1.5, barrel.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
		barrel.getWorld().playSound(barrel.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
	}

	private void createHologram(Block block) {
		Location above = block.getRelative(BlockFace.UP).getLocation();
		above.setX(above.getX() + 0.5);
		above.setY(above.getY() + 1.0);
		above.setZ(above.getZ() + 0.5);
		hologram = HologramsAPI.createHologram(Brewery.breweryDriver, above);
	}

	private void updateHologram() {
		if(hologram != null)
		{
			int tempYear = (int)(time/minutesPerYear);		
			hologram.clearLines();
			hologram.appendTextLine(type.toString() + " barrel");
			hologram.appendTextLine("Aged " + tempYear + " year" + (tempYear == 1 ? "" : "s"));
		}
	}

	public boolean hasPermsOpen(Player player, PlayerInteractEvent event) {
		if (!player.hasPermission("brewery.openbarrel.big")) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Error_NoBarrelAccess"));
			return false;
		}
		return true;
	}

	// removes a barrel, throwing included potions to the ground
	public void removeAndFinishBrewing(Block broken, Player breaker) {
		for (HumanEntity human : barrel.getInventory().getViewers()) {
			human.closeInventory();
		}	
		barrel.getInventory().clear();
		ItemStack finalBrew = BrewItemFactory.getAgedBrew(mash, (int)(time/minutesPerYear), type);

		// Set Items
		for(int i = 0; i < numberOfBrews; i++)
		{
			barrel.getInventory().addItem(finalBrew);
		}
		
		// Update Hologram if possible
		if(hologram != null)
		{	
			hologram.delete();
		}
		barrels.remove(this);
	}

	public void removeSelf() {
		if(hologram != null)
		{
			hologram.delete();
		}
		barrels.remove(this);
	}


	//////////////////////////////////////////////////////////////////////////
	public static BreweryBarrel getBarrel(Block block) {
		for(BreweryBarrel barrel : barrels) {
			if(barrel.barrel.getBlock().equals(block)) {
				return barrel;
			}
		}
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
			barrel.playAgingEffect();
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

				String query = "REPLACE " + Brewery.getDatabase("barrels")
				+ "barrels SET idbarrels=?, location=?, type=?, time=?";
				try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
					stmt.setInt(1, id);
					stmt.setString(2, location);
					stmt.setString(3, barrel.type.name());
					stmt.setFloat(4, barrel.time);

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

	public static boolean isBarrelLid(ItemStack item)
	{
		switch (item.getType()) {
		case OAK_TRAPDOOR:
		case DARK_OAK_TRAPDOOR:
		case ACACIA_TRAPDOOR:
		case JUNGLE_TRAPDOOR:
		case BIRCH_TRAPDOOR:
		case SPRUCE_TRAPDOOR:
			return true;
		default:
			return false;
		}
	}
	//@precondition: is a Barrel
	public static boolean isValidAgingBarrel(Player player, Block block)
	{
		Inventory inventory = ((Container)block.getState()).getInventory();
		// Fermented Mash Bucket
		if(inventory.getItem(0) == null ||
				!MashBucket.isFermentedBucket(inventory.getItem(0)))
		{
			Brewery.breweryDriver.msg(player, "You need a fermented mash bucket in the first slot!");
			return false;
		}

		// Water Bottle
		if(inventory.getItem(1) == null ||
				inventory.getItem(1).getType() != Material.GLASS_BOTTLE)
		{
			Brewery.breweryDriver.msg(player, "You need empty bottles in the second slot!");
			return false;
		}
		return true;
	}
}
