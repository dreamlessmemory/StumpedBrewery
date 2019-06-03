package com.dreamless.brewery.entity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.utils.BreweryMessage;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;

public class BreweryBarrel {

	public static HashSet<BreweryBarrel> barrels = new HashSet<BreweryBarrel>();

	// Difficulty adjustments
	public static double minutesPerYear = 20.0;

	private Barrel barrel;
	private float time;
	private Hologram hologram;
	private boolean aging = false;
	private BarrelType type;

	public BreweryBarrel(Barrel block, BarrelType type, float time, boolean aging) {
		this.barrel = block;
		this.time = time;
		this.aging = aging;
		this.type = type;

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
		hologram.appendTextLine(type.getBarrelName() + " barrel");
		if (aging) {
			hologram.appendTextLine("Aged " + (int) time + " years");
		} else {
			hologram.appendTextLine("Ready to age");
		}
	}

	private String getWoodName() {
		// TODO Auto-generated method stub
		return "TEST";
	}

	public BreweryMessage startAging(Player player) {
		Inventory inventory = barrel.getInventory();
		ItemStack[] contentsStack = inventory.getContents();
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack item = contentsStack[i];
			if (item == null)
				continue;
			NBTItem nbti = new NBTItem(item);
			if (!nbti.hasKey("brewery")) {// eject if not a brewery item
				barrel.getWorld().dropItemNaturally(barrel.getBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5),
						item);
				inventory.remove(item);
				continue;
			} else {
				NBTCompound brewery = nbti.getCompound("brewery");
				if (brewery.hasKey("aged") || brewery.hasKey("ruined")) {// eject if aged already
					barrel.getWorld().dropItemNaturally(barrel.getBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5),
							item);
					inventory.remove(item);
					continue;
				}

				brewery.setBoolean("aged", true);
				brewery.setString("placedInBrewer", player.getUniqueId().toString());
				item = nbti.getItem();
				inventory.setItem(i, item);

			}
		}

		if (isEmpty()) {
			return new BreweryMessage(false, Brewery.getText("Barrel_Empty"));
		} else {
			aging = true;

			if (hologram == null) {
				createHologram(barrel.getBlock());
			}
			updateHologram();

			return new BreweryMessage(true, Brewery.getText("Barrel_Start_Aging"));
		}
	}

	private void ageContents(double time) {
		ItemStack[] contents = barrel.getInventory().getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			// Brewery.breweryDriver.debugLog("Pull item");
			if (item == null) {
				continue;
			}
			contents[i] = ageOneYear(item, getWood(), time);

			// Update Inventory
		}
		barrel.getInventory().setContents(contents);
	}

	private byte getWood() {
		// TODO Auto-generated method stub
		return 0;
	}

	private ItemStack ageOneYear(ItemStack item, byte woodType, double time) {

		Brewery.breweryDriver.debugLog("AGING 1 YEAR : " + item.toString());

		// Pull NBT
		NBTItem nbti = new NBTItem(item);
		NBTCompound brewery = nbti.getCompound("brewery");

		// Adjust multipliers

		switch (woodType) {
		case 1:// birch
			brewery.setInteger("potency", brewery.getInteger("potency") - 4);
			brewery.setInteger("duration", brewery.getInteger("duration") + 4);
			break;
		case 2: // Oak
			brewery.setInteger("potency", brewery.getInteger("potency") + 4);
			brewery.setInteger("duration", brewery.getInteger("duration") - 4); // - 0.05
			break;
		case 3: // Jungle
			brewery.setInteger("potency", brewery.getInteger("potency") + 8);
			brewery.setInteger("duration", brewery.getInteger("duration") - 8);
			break;
		case 4: // Spruce
			brewery.setInteger("potency", brewery.getInteger("potency") + 6);
			brewery.setInteger("duration", brewery.getInteger("duration") - 6);
			break;
		case 5: // Acacia
			brewery.setInteger("potency", brewery.getInteger("potency") - 8);
			brewery.setInteger("duration", brewery.getInteger("duration") + 8);
			break;
		case 6: // Dark Oak
			brewery.setInteger("potency", brewery.getInteger("potency") + 6);
			brewery.setInteger("duration", brewery.getInteger("duration") - 6);
			break;
		default:
			break;
		}

		item = nbti.getItem();

		// Mask as Aging Brew
		// int age = (int) Math.floor(aging.getDouble("age"));
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		potionMeta.setDisplayName("Aging Brew");
		ArrayList<String> agedFlavorText = new ArrayList<String>();
		agedFlavorText.add("An aging " + brewery.getString("type").toLowerCase() + " brew.");
		agedFlavorText.add("This brew has aged for " + (int) time + " years");
		potionMeta.setLore(agedFlavorText);
		potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(potionMeta);

		return item;

	}

	public boolean isEmpty() {
		for (ItemStack it : barrel.getInventory().getContents()) {
			if (it != null)
				return false;
		}
		return true;
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

			for (ItemStack item : items) {
				if (item != null) {
					// "broken" is the block that destroyed, throw them there!
					item = BRecipe.revealMaskedBrew(item);
					if (broken != null) {
						broken.getWorld().dropItemNaturally(broken.getLocation().add(0.5, 0.5, 0.5), item);
						broken.getWorld().playSound(broken.getLocation(), Sound.ENTITY_ITEM_PICKUP,
								(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
					} else {
						barrel.getWorld().dropItemNaturally(barrel.getLocation().add(0.5, 0.5, 0.5), item);
						barrel.getWorld().playSound(barrel.getLocation(), Sound.ENTITY_ITEM_PICKUP,
								(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
					}
				}
			}
		hologram.delete();
		barrels.remove(this);
	}

	public boolean isAging() {
		return aging;
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
			if (barrel.isAging()) {
				double newTime = barrel.time + (1.0 / minutesPerYear);
	
				// So, if the new time has ticked over at least a year
				if (Math.floor(newTime) - Math.floor(barrel.time) >= 1) {
					barrel.ageContents(Math.floor(newTime));
				}
				barrel.time = (float) newTime;
				barrel.updateHologram();
			}
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
						+ "barrels SET idbarrels=?, location=?, inventory=?, time=?, aging=?";
				try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
					stmt.setInt(1, id);
					stmt.setString(2, location);
					stmt.setFloat(4, barrel.time);
					stmt.setBoolean(5, barrel.aging);
	
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

	public enum BarrelType {
		BASIC, TYPE1;
		
		public final String getBarrelName() {
			switch(this) {
			case BASIC:
				return "Basic";
			case TYPE1:
				return "Type 1";
			default:
				return "test";
			
			}
		}
		
	}
}
