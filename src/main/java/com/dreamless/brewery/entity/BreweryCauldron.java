package com.dreamless.brewery.entity;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitTask;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.recipe.AspectOld;
import com.dreamless.brewery.recipe.BreweryIngredient.Aspect;
import com.dreamless.brewery.utils.BreweryMessage;
import com.dreamless.brewery.utils.BreweryUtils;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BreweryCauldron implements InventoryHolder {
	private static CopyOnWriteArrayList<BreweryCauldron> cauldronList = new CopyOnWriteArrayList<BreweryCauldron>();
	private static HashMap<BreweryCauldron, BukkitTask> taskMap = new HashMap<>();

	// private BIngredients ingredients = new BIngredients();
	private final Block block;
	private int cookTime = 0; // Seconds
	private boolean cooking = false;
	private Hologram hologram;

	// Ingredients
	private Inventory inventory;
	//private HashMap<String, AspectOld> aspectMap = new HashMap<String, AspectOld>();
	//private String type;

	public BreweryCauldron(Block block) {
		this.block = block;
		cooking = false;
		inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
		cauldronList.add(this);
		createHologram(block);
		updateHologram();
	}

	// loading from file
	public BreweryCauldron(Block block, int secondsCooked, boolean cooking, String inventoryString,
			HashMap<String, AspectOld> aspects) { //TODO: Redo aspect save/load
		this.block = block;
		this.cookTime = secondsCooked;
		this.cooking = cooking;

		//this.aspectMap = aspects;
		this.cooking = cooking;

		// Initialize Inventory
		try {
			inventory = BreweryUtils.fromBase64(inventoryString, this);
		} catch (IOException e) {
			inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
			Brewery.breweryDriver.debugLog("Error creating inventory for a cauldron");
			e.printStackTrace();
		}

		// Initialize
		// Add to lists
		cauldronList.add(this);
		if (cooking) {
			BukkitTask task = Bukkit.getScheduler().runTaskTimer(Brewery.breweryDriver,
					new BreweryCauldronRunnable(this), 0, 20);
			taskMap.put(this, task);
		}

		// Start Hologram
		if (hologram == null) {
			createHologram(block);
		}
		updateHologram();

	}

	// Retrieve cauldron from list by Block
	public static BreweryCauldron get(Block block) {
		for (BreweryCauldron bcauldron : cauldronList) {
			if (bcauldron.block.equals(block)) {
				return bcauldron;
			}
		}
		return null;
	}

	// reset to normal cauldron
	public static void remove(Block block) {
		BreweryCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (!bcauldron.cooking) {
				bcauldron.dumpContents();
			}
			cauldronList.remove(bcauldron);
	
			// Remove hologram
			bcauldron.hologram.delete();
		}
	}

	public static void remove(BreweryCauldron bcauldron) {
		if (!bcauldron.cooking) {
			bcauldron.dumpContents();
		}
		cauldronList.remove(bcauldron);
	
		// Remove hologram
		bcauldron.hologram.delete();
	}

	/*
	public Inventory getInventory(Block block) {
		BreweryCauldron bcauldron = get(block);
		if (bcauldron == null) {
			if (getFillLevel(block) > 1) {
				bcauldron = new BreweryCauldron(block);
			} else {
				return null;
			}
		}
		return bcauldron.getInventory();
	}*/
	
	// unloads cauldrons that are in a unloading world
	// as they were written to file just before, this is safe to do
	public static void onUnload(String name) {
		for (BreweryCauldron bcauldron : cauldronList) {
			if (bcauldron.block.getWorld().getName().equals(name)) {
				cauldronList.remove(bcauldron);
			}
		}
	}

	/*
	public Inventory getInventory(Block block) {
		BreweryCauldron bcauldron = get(block);
		if (bcauldron == null) {
			if (getFillLevel(block) > 1) {
				bcauldron = new BreweryCauldron(block);
			} else {
				return null;
			}
		}
		return bcauldron.getInventory();
	}*/
	
	public static void onDisable() {
		cauldronList.clear();
	}

	// fills players bottle with cooked brew
	public static boolean fillBottle(Player player, Block block) {
		BreweryCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (!player.hasPermission("brewery.cauldron.fill")) {
				Brewery.breweryDriver.msg(player, Brewery.getText("Perms_NoCauldronFill"));
				return true;
			}
			ItemStack potion = bcauldron.finishFermentation(bcauldron.cookTime, player);

			if (potion != null) {

				Levelled cauldronData = (Levelled) block.getBlockData();
				int level = cauldronData.getLevel();

				if (level > cauldronData.getMaximumLevel()) {
					level = cauldronData.getMaximumLevel();
				} else if (level <= 0) {
					cauldronList.remove(bcauldron);
					// Remove hologram
					bcauldron.hologram.delete();
					return false;
				}

				cauldronData.setLevel(--level);
				block.setBlockData(cauldronData);
				block.getState().update();

				if (level == 0) {
					cauldronList.remove(bcauldron);
					// Remove hologram
					bcauldron.hologram.delete();
				}
				player.getInventory().addItem(potion);
				// giveItem(player, potion);
				return true;
			}
		}
		return false;
	}
	
	public static boolean isUseableCauldron (Block block) {
		Material down = block.getRelative(BlockFace.DOWN).getType();
		Material up = block.getRelative(BlockFace.UP).getType();

		if (down != Material.FIRE && down != Material.LAVA && down != Material.MAGMA_BLOCK
				&& down != Material.CAMPFIRE) {
			return false;
		}

		if (up != Material.AIR) {
			return false;
		}
		return true;
	}

	// prints the current cooking time to the player
	public void printTime(Player player) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Error_NoPermissions"));
			return;
		}
		
		if (cookTime > 1) {
			Brewery.breweryDriver.msg(player,Brewery.getText("Player_CauldronInfo2", Integer.toString(cookTime)));
		} else if (cookTime == 1) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Player_CauldronInfo1"));
		} else {
			Brewery.breweryDriver.msg(player, Brewery.getText("Player_CauldronInfo0"));
		}
	}

	
	// 0 = empty, 1 = something in, 2 = full
	public int getFillLevel() {
		return ((Levelled)block.getState().getBlockData()).getLevel();
	}

	public BreweryMessage startCooking() {
		// Set Feedback effects
		block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().getX() + 0.5,
				block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);

		// Manage parameters
		cooking = true;
		
		// Create hologram
		if (hologram == null) {
			createHologram(block);
		}
		updateHologram();
		
		// Return
		return new BreweryMessage(true, Brewery.getText("Fermentation_Start_Fermenting") + ".");
	}

	public void purgeContents() {
		ItemStack[] contents = inventory.getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if (item != null) {
				if (Aspect.getAspect(item.getType()) != Aspect.INVALID) {
					if (usesBucket(item)) {
						dumpItem(new ItemStack(Material.BUCKET));
					}
					// TODO: Add Ingredient
					//addIngredient(item);
				} else {// eject
					dumpItem(item);
					contents[i] = null;
				}
			}
		}
		inventory.setContents(contents);
	}

	public void onUpdate() {// UPDATE THE POTION
		// Check if fire still alive
		if ((!block.getChunk().isLoaded() || fireAndAirInPlace().getResult()) && cooking) {

			if (getFillLevel() == 0) {// remove yourself if empty
				remove(block);
				return;
			}

			// Update Sign
			updateHologram();

			++cookTime;
			// Bubble effects
			if (Math.random() > Brewery.effectLevel) {
				block.getWorld().spawnParticle(Particle.SMOKE_NORMAL, block.getLocation().getX() + 0.5,
						block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 20, 0.15, 0.15, 0.15, 0.05);
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BREWING_STAND_BREW,
						(float) (Math.random() / 8) + 0.1f, (float) (Math.random() * 1.5) + 0.5f);
			} else if (Math.random() > Brewery.effectLevel) {
				// Sound and particle effects
				block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().getX() + 0.5,
						block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE,
						(float) (Math.random() / 8) + 0.1f, (float) (Math.random() / 2) + 0.75f);
			}
		} else { // no fire, stop cooking
			if (cooking) {
				cooking = false;
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 2.0f, 1.0f);
				updateHologram();
				hologram.appendTextLine("Fermentation stopped");
			}
		}
	}

	public boolean isCooking() {
		return cooking;
	}
	
	public BreweryMessage addIngredient(Material ingredient) {
		if(Aspect.getAspect(ingredient) != Aspect.INVALID) // Unnecessary?
		{
			if(!inventory.addItem(new ItemStack(ingredient)).isEmpty()) {
				return new BreweryMessage(false, Brewery.getText("Fermentation_No_Space"));
			}
			return new BreweryMessage(true, ""); 
		}
		return new BreweryMessage(false, Brewery.getText("Fermentation_Not_Valid"));
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
	
	/**
	 * PRIVATE METHODS
	 */

	private BreweryMessage fireAndAirInPlace() {

		Material down = block.getRelative(BlockFace.DOWN).getType();
		Material up = block.getRelative(BlockFace.UP).getType();

		if (down != Material.FIRE && down != Material.LAVA && down != Material.MAGMA_BLOCK
				&& down != Material.CAMPFIRE) {
			return new BreweryMessage(false, Brewery.getText("Fermentation_No_Heat"));
		}

		if (up != Material.AIR) {
			return new BreweryMessage(false, Brewery.getText("Fermentation_No_Space_Above"));
		}

		return new BreweryMessage(true, "");
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

		int secondsCooked = cookTime % 60;

		// Time
		hologram.appendTextLine(cookTime + ":" + (secondsCooked < 10 ? "0" : "") + secondsCooked);

		// Status
		if (cooking) {
			hologram.appendTextLine("Cooking...");
		}
		if (fireAndAirInPlace().getResult()) {
			hologram.appendTextLine("Ready...");
		}

	}

	private void dumpContents() {
		for (ItemStack item : inventory.getContents()) {
			if (item != null) {
				dumpItem(item);
				inventory.remove(item);
			}
		}
	}

	private void dumpItem(ItemStack item) {
		if (item != null) {
			block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5), item);
			block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP,
					(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
		}
	}

	private boolean usesBucket(ItemStack item) {
		switch (item.getType()) {
		case LAVA_BUCKET:
		case MILK_BUCKET:
		case WATER_BUCKET:
			return true;
		default:
			return false;
		}
	}

	private ItemStack finishFermentation(int state, Player player) {

		// Stop Task timer
		taskMap.get(this).cancel();

		// Get type
		//calculateType();

		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// Set Name

		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound("brewery"); // All brewery NBT gets set here.

		// TODO: Assign aspects

		// Multipliers
		breweryMeta.setInteger("potency", 100);
		breweryMeta.setInteger("duration", 100);

		// Type
		//breweryMeta.setString("type", type);

		// Crafter
		NBTCompound crafters = breweryMeta.addCompound("crafters");
		crafters.setString(player.getDisplayName(), player.getDisplayName());

		// Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}

	public class BreweryCauldronRunnable implements Runnable {

		private BreweryCauldron cauldron;

		public BreweryCauldronRunnable(BreweryCauldron cauldron) {
			this.cauldron = cauldron;
		}

		@Override
		public void run() {
			cauldron.onUpdate();
		}
	}
}
