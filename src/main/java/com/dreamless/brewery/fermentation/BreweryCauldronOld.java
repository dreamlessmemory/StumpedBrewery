package com.dreamless.brewery.fermentation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import com.dreamless.brewery.database.DatabaseCache;
import com.dreamless.brewery.recipe.AspectOld;
import com.dreamless.brewery.recipe.AspectOld.AspectRarity;
import com.dreamless.brewery.utils.BreweryMessage;
import com.dreamless.brewery.utils.BreweryUtils;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

@Deprecated
public class BreweryCauldronOld implements InventoryHolder {
	private static CopyOnWriteArrayList<BreweryCauldronOld> bcauldrons = new CopyOnWriteArrayList<BreweryCauldronOld>();
	private static HashMap<BreweryCauldronOld, BukkitTask> taskMap = new HashMap<>();

	// private BIngredients ingredients = new BIngredients();
	private final Block block;
	private int cookTime = 0; // Seconds
	private boolean cooking = false;
	private Hologram hologram;

	// Ingredients
	private Inventory inventory;
	private HashMap<String, AspectOld> aspectMap = new HashMap<String, AspectOld>();
	private String type;
	// private ItemStack coreIngredient;
	// private ItemStack adjunctIngredient;

	// private String coreIngredient = "";
	// private String adjunctIngredient = "";
	// private int coreAmount = 0;
	// private int adjunctAmount = 0;

	public BreweryCauldronOld(Block block) {
		this.block = block;
		cooking = false;
		bcauldrons.add(this);
	}

	// loading from file
	public BreweryCauldronOld(Block block, int minutesCooked, boolean cooking, String inventoryString,
			HashMap<String, AspectOld> aspects) {
		this.block = block;
		this.cookTime = minutesCooked;
		// this.ingredients = ingredients;
		this.cooking = cooking;

		this.aspectMap = aspects;
		this.cooking = cooking;

		// Initialize Inventory
		/*try {
			inventory = BreweryUtils.fromBase64(inventoryString, this);
		} catch (IOException e) {
			inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
			Brewery.breweryDriver.debugLog("Error creating inventory for a cauldron");
			e.printStackTrace();
		}*/

		// Initialize
		// determineCoreAndAdjunct(inventory.getContents());

		// Add to lists
		bcauldrons.add(this);
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

	// get cauldron by Block
	public static BreweryCauldronOld get(Block block) {
		for (BreweryCauldronOld bcauldron : bcauldrons) {
			if (bcauldron.block.equals(block)) {
				return bcauldron;
			}
		}
		return null;
	}

	// fills players bottle with cooked brew
	public static boolean fill(Player player, Block block) {
		BreweryCauldronOld bcauldron = get(block);
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
					bcauldrons.remove(bcauldron);
					// Remove hologram
					bcauldron.hologram.delete();
					return false;
				}

				cauldronData.setLevel(--level);
				block.setBlockData(cauldronData);
				block.getState().update();

				if (level == 0) {
					bcauldrons.remove(bcauldron);
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

	// 0 = empty, 1 = something in, 2 = full
	public static int getFillLevel(Block block) {
		if (block.getType() == Material.CAULDRON) {
			Levelled fillLevel = (Levelled) block.getState().getBlockData();
			return fillLevel.getLevel();
		}
		return 0;
	}

	// prints the current cooking time to the player
	public static void printTime(Player player, Block block) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Error_NoPermissions"));
			return;
		}
		BreweryCauldronOld bcauldron = get(block);
		if (bcauldron != null) {
			if (bcauldron.cookTime > 1) {
				Brewery.breweryDriver.msg(player,
						Brewery.getText("Player_CauldronInfo2", Integer.toString(bcauldron.cookTime)));
			} else if (bcauldron.cookTime == 1) {
				Brewery.breweryDriver.msg(player, Brewery.getText("Player_CauldronInfo1"));
			} else {
				Brewery.breweryDriver.msg(player, Brewery.getText("Player_CauldronInfo0"));
			}
		}
	}

	public static Inventory getInventory(Block block) {
		BreweryCauldronOld bcauldron = get(block);
		if (bcauldron == null) {
			if (getFillLevel(block) > 1) {
				bcauldron = new BreweryCauldronOld(block);
			} else {
				return null;
			}
		}
		return bcauldron.getInventory();
	}

	// reset to normal cauldron
	public static void remove(Block block) {
		BreweryCauldronOld bcauldron = get(block);
		if (bcauldron != null) {
			if (!bcauldron.cooking) {
				bcauldron.dumpContents();
			}
			bcauldrons.remove(bcauldron);

			// Remove hologram
			bcauldron.hologram.delete();
		}
	}

	public static void remove(BreweryCauldronOld bcauldron) {
		if (!bcauldron.cooking) {
			bcauldron.dumpContents();
		}
		bcauldrons.remove(bcauldron);

		// Remove hologram
		bcauldron.hologram.delete();
	}

	// unloads cauldrons that are in a unloading world
	// as they were written to file just before, this is safe to do
	public static void onUnload(String name) {
		for (BreweryCauldronOld bcauldron : bcauldrons) {
			if (bcauldron.block.getWorld().getName().equals(name)) {
				bcauldrons.remove(bcauldron);
			}
		}
	}

	public static void save() {
		int id = 0;
		if (!bcauldrons.isEmpty()) {
			for (BreweryCauldronOld cauldron : bcauldrons) {
				Brewery.breweryDriver.debugLog("CAULDRON");
				if (((Levelled) cauldron.block.getBlockData()).getLevel() < 1) {
					Brewery.breweryDriver.debugLog("Skipping saving, empty");
					continue;
				}
				// Location
				String location = Brewery.gson.toJson(cauldron.block.getLocation().serialize());
				Brewery.breweryDriver.debugLog(location);

				String jsonInventory = null;
				// inventory
				try {
					jsonInventory = BreweryUtils.toBase64(cauldron.getInventory());
					Brewery.breweryDriver.debugLog(jsonInventory);
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}

				// Aspects
				String aspects = Brewery.gson.toJson(cauldron.getAspects());
				Brewery.breweryDriver.debugLog(aspects);

				// TODO: Data save
				String query = "REPLACE " + Brewery.getDatabase("cauldrons")
						+ "cauldrons SET idcauldrons=?, location=?, contents=?, aspects=?, state=?, cooking=?, lastCook=?";
				try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
					stmt.setInt(1, id);
					stmt.setString(2, location);
					stmt.setString(3, jsonInventory);
					stmt.setString(4, aspects);
					stmt.setInt(5, cauldron.cookTime);
					stmt.setBoolean(6, cauldron.cooking);

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
		String query = "DELETE FROM " + Brewery.getDatabase("cauldrons") + "cauldrons WHERE idcauldrons >=?";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setInt(1, id);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}
	}

	// bukkit bug not updating the inventory while executing event, have to
	// schedule the give
	/*
	 * public static void giveItem(final Player player, final ItemStack item) {
	 * Brewery.breweryDriver.getServer().getScheduler().runTaskLater(Brewery.
	 * breweryDriver, new Runnable() { public void run() {
	 * player.getInventory().addItem(item); } }, 1L); }
	 */

	public static boolean isCooking(Block block) {
		BreweryCauldronOld bcauldron = get(block);
		if (bcauldron != null) {
			return bcauldron.cooking;
		} else {
			return false;
		}
	}

	public BreweryMessage startCooking(Player player) {
		// Set Feedback effects
		block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().getX() + 0.5,
				block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);

		// Manage parameters
		cooking = true;
		// determineCoreAndAdjunct(contents);

		// Calculate type
		// calculateType(0);

		// Create hologram
		if (hologram == null) {
			createHologram(block);
		}
		updateHologram();
		// Return
		return new BreweryMessage(true, Brewery.getText("Fermentation_Start_Fermenting") + type.toLowerCase() + ".");
	}

	public void purgeContents() {
		ItemStack[] contents = inventory.getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if (item != null) {
				if (DatabaseCache.containsIngredient(item.getType())) {
					if (usesBucket(item)) {
						dumpItem(new ItemStack(Material.BUCKET));
					}
					addIngredient(item);
				} else {// eject
					dumpItem(item);
					contents[i] = null;
				}
			}
		}
		inventory.setContents(contents);
	}

	public static void onDisable() {
		bcauldrons.clear();
	}

	public boolean isCooking() {
		return cooking;
	}

	public void onUpdate() {// UPDATE THE POTION
		// Check if fire still alive
		if ((!block.getChunk().isLoaded() || fireAndAirInPlace().getResult()) && cooking) {

			if (getFillLevel(block) == 0) {// remove yourself if empty
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

	@Override
	public Inventory getInventory() {
		return inventory;
	}

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

	private HashMap<String, AspectOld> getAspects() {
		return aspectMap;
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

	private void addIngredient(ItemStack ingredient) {
		// SQL
		String aspectQuery = "name, aspect1name, aspect1rating, aspect2name, aspect2rating, aspect3name, aspect3rating";
		String query = "SELECT " + aspectQuery + " FROM " + Brewery.getDatabase(null) + "ingredients WHERE name=?";

		// Aspect multipliers
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setString(1, ingredient.getType().name());
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("Failed to poll SQL");
			} else {// Successful Pull
				for (int i = 1; i <= 3; i++) {
					String name = results.getString("aspect" + i + "name");
					if (name == null)
						continue;
					AspectOld aspect = aspectMap.get(name);
					int multiplier = ingredient.getAmount();
					AspectRarity values = AspectOld.getRarityValues(results.getInt("aspect" + i + "rating"));
					if (aspect != null) {// aspect is found
						aspect.setValues(values.getPotency() * multiplier + aspect.getPotency(),
								values.getSaturation() * multiplier + aspect.getSaturation());
					} else {
						aspectMap.put(name,
								new AspectOld(values.getPotency() * multiplier, values.getSaturation() * multiplier));
					}
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	private String queryForType(String primary, int primaryAmount, String secondary, int secondaryAmount, int time) {
		String result = null;
		String query = "SELECT type FROM " + Brewery.getDatabase("brewtypes")
				+ "brewtypes WHERE core=? AND adjunct=? GROUP BY type "
				+ "HAVING MAX(coreamount) <= ? AND MAX(adjunctamount) <= ? AND MAX(time) <=? "
				+ "ORDER BY time DESC LIMIT 1";

		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			// Set values
			stmt.setString(1, primary);
			stmt.setString(2, secondary);
			stmt.setInt(3, primaryAmount);
			stmt.setInt(4, secondaryAmount);
			stmt.setInt(5, time);

			Brewery.breweryDriver.debugLog(stmt.toString());

			// Retrieve results
			ResultSet results;
			results = stmt.executeQuery();
			if (results.next()) {
				result = results.getString("type");
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		return result;
	}

	private void calculateType() {
		// determineCoreAndAdjunct(inventory.getContents());

		ItemStack coreIngredient = inventory.getItem(0);
		ItemStack adjunctIngredient = inventory.getItem(1);

		//

		String result = queryForType(coreIngredient.getType().name(), coreIngredient.getAmount(),
				adjunctIngredient.getType().name(), adjunctIngredient.getAmount(), cookTime);

		if (result != null) {// There is a secondary
			type = result;
		} else {
			result = queryForType(coreIngredient.getType().name(), 1, "", 0, 0);
			if (result == null) {
				type = "ELIXR";
			} else {
				type = result;
			}
		}

		Brewery.breweryDriver.debugLog("Resultant brew: " + type);
	}

	private ItemStack finishFermentation(int state, Player player) {

		// Stop Task timer
		taskMap.get(this).cancel();

		// Get type
		calculateType();

		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// Set Name

		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound("brewery"); // All brewery NBT gets set here.

		// TODO: Assign aspects
		// NBTCompound aspectTagList = breweryMeta.addCompound("aspectsBase");
		// NBTCompound aspectActList = breweryMeta.addCompound("aspectsActivation");
		// for(Entry<String, Double> entry: calculatedActivation.entrySet()) {
		// aspectTagList.setDouble(entry.getKey(),
		// aspectMap.get(entry.getKey()).getCookedBase());
		// aspectActList.setDouble(entry.getKey(), entry.getValue());
		// }

		// Multipliers
		breweryMeta.setInteger("potency", 100);
		breweryMeta.setInteger("duration", 100);

		// Type
		breweryMeta.setString("type", type);

		// Crafter
		NBTCompound crafters = breweryMeta.addCompound("crafters");
		crafters.setString(player.getDisplayName(), player.getDisplayName());

		// Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}

	public class BreweryCauldronRunnable implements Runnable {

		private BreweryCauldronOld cauldron;

		public BreweryCauldronRunnable(BreweryCauldronOld cauldron) {
			this.cauldron = cauldron;
		}

		@Override
		public void run() {
			cauldron.onUpdate();
		}
	}
}