package com.dreamless.brewery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.dreamless.brewery.filedata.*;
import com.dreamless.brewery.listeners.*;
import com.mysql.jdbc.Connection;
//import com.mysql.jdbc.PreparedStatement;

public class Brewery extends JavaPlugin {
	public static Brewery breweryDriver;
	public static boolean debug;
	public static boolean useUUID;
	public static boolean use1_9;
	public static boolean updateCheck;

	// Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	public InventoryListener inventoryListener;
	public WorldListener worldListener;

	// Language
	public String language;
	public LanguageReader languageReader;

	private CommandSender reloader;
	
	//DataBase vars.
	final String username="mchost_214986"; //Enter in your db username
	final String password="StumpedDB!"; //Enter your password for the db
	final String url = "jdbc:mysql://mu1.mchost.pro:3306/mchost_214986"; //Enter URL w/db name

	//Connection vars
	static Connection connection; //This is the variable we will use to connect to database

	@Override
	public void onEnable() {
		breweryDriver = this;

		// Version check
		String v = Bukkit.getBukkitVersion();
		useUUID = !v.matches("(^|.*[^\\.\\d])1\\.[0-6]([^\\d].*|$)") && !v.matches("(^|.*[^\\.\\d])1\\.7\\.[0-5]([^\\d].*|$)");
		use1_9 = !v.matches("(^|.*[^\\.\\d])1\\.[0-8]([^\\d].*|$)");
		
		//Server Check
		try { //We use a try catch to avoid errors, hopefully we don't get any.
		    Class.forName("com.mysql.jdbc.Driver"); //this accesses Driver in jdbc.
		} catch (ClassNotFoundException e) {
		    e.printStackTrace();
		    System.err.println("jdbc driver unavailable!");
		    return;
		}
		try { //Another try catch to get any SQL errors (for example connections errors)
		    connection = (Connection) DriverManager.getConnection(url,username,password);
		    //with the method getConnection() from DriverManager, we're trying to set
		    //the connection's url, username, password to the variables we made earlier and
		    //trying to get a connection at the same time. JDBC allows us to do this.
		} catch (SQLException e) { //catching errors)
		    e.printStackTrace(); //prints out SQLException errors to the console (if any)
		}
		
		//On enable, try to get something.
		try {
			String sql = "SELECT * FROM ingredients WHERE name='WHEAT_SEEDS'";
			PreparedStatement stmt;
			stmt = connection.prepareStatement(sql);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
			    System.out.println("Failed");
			} else {
				int rating = results.getInt("aspect2rating");
			    debugLog("Success " + rating);
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		// load the Config
		try {
			if (!readConfig()) {
				breweryDriver = null;
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			breweryDriver = null;
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		readData();

		// Listeners
		blockListener = new BlockListener();
		playerListener = new PlayerListener();
		entityListener = new EntityListener();
		inventoryListener = new InventoryListener();
		worldListener = new WorldListener();
		getCommand("Brewery").setExecutor(new CommandListener());

		breweryDriver.getServer().getPluginManager().registerEvents(blockListener, breweryDriver);
		breweryDriver.getServer().getPluginManager().registerEvents(playerListener, breweryDriver);
		breweryDriver.getServer().getPluginManager().registerEvents(entityListener, breweryDriver);
		breweryDriver.getServer().getPluginManager().registerEvents(inventoryListener, breweryDriver);
		breweryDriver.getServer().getPluginManager().registerEvents(worldListener, breweryDriver);
		if (use1_9) {
			breweryDriver.getServer().getPluginManager().registerEvents(new CauldronListener(), breweryDriver);
		}

		// Heartbeat
		breweryDriver.getServer().getScheduler().runTaskTimer(breweryDriver, new BreweryRunnable(), 650, 1200);
		breweryDriver.getServer().getScheduler().runTaskTimer(breweryDriver, new DrunkRunnable(), 120, 120);

		if (updateCheck) {
			breweryDriver.getServer().getScheduler().runTaskLaterAsynchronously(breweryDriver, new UpdateChecker(), 135);
		}

		this.log(this.getDescription().getName() + " enabled!");
	}

	@Override
	public void onDisable() {
		
		// Disable Server
	    try { //using a try catch to catch connection errors (like wrong sql password...)
	        if (connection!=null && !connection.isClosed()){ //checking if connection isn't null to
	            //avoid receiving a nullpointer
	            connection.close(); //closing the connection field variable.
	        }
	    } catch(Exception e) {
	        e.printStackTrace();
	    }

		// Disable listeners
		HandlerList.unregisterAll(this);

		// Stop shedulers
		getServer().getScheduler().cancelTasks(this);

		if (breweryDriver == null) {
			return;
		}

		// save Data to Disk
		DataSave.save(true);

		// save LanguageReader
		languageReader.save();

		// delete Data from Ram
		Barrel.barrels.clear();
		BCauldron.bcauldrons.clear();
		BRecipe.recipes.clear();
		//BIngredients.cookedNames.clear();
		BPlayer.clear();
		Brew.potions.clear();
		Wakeup.wakeups.clear();
		Words.words.clear();
		Words.ignoreText.clear();
		Words.commands = null;

		this.log(this.getDescription().getName() + " disabled!");
	}

	public void reload(CommandSender sender) {
		if (sender != null && !sender.equals(getServer().getConsoleSender())) {
			reloader = sender;
		}
		// clear all existent config Data
		BRecipe.recipes.clear();
		//BIngredients.cookedNames.clear();
		Words.words.clear();
		Words.ignoreText.clear();
		Words.commands = null;
		BPlayer.drainItems.clear();

		try {
			if (!readConfig()) {
				breweryDriver = null;
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			breweryDriver = null;
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// save and load LanguageReader
		languageReader.save();
		languageReader = new LanguageReader(new File(breweryDriver.getDataFolder(), "languages/" + language + ".yml"));

		// Reload Recipes
		Boolean successful = true;
		for (Brew brew : Brew.potions.values()) {
			if (!brew.reloadRecipe()) {
				successful = false;
			}
		}
		if (!successful) {
			msg(sender, breweryDriver.languageReader.get("Error_Recipeload"));
		}
		reloader = null;
	}

	public void msg(CommandSender sender, String msg) {
		sender.sendMessage(color("&2[Brewery] &f" + msg));
	}
	
	public void msgMult(CommandSender sender, String msg) {
			sender.sendMessage(msg.split("\n"));
	}

	public void log(String msg) {
		this.msg(Bukkit.getConsoleSender(), msg);
	}

	public void debugLog(String msg) {
		if (debug) {
			this.msg(Bukkit.getConsoleSender(), "&2[Debug] &f" + msg);
		}
	}

	public void errorLog(String msg) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + msg);
		if (reloader != null) {
			reloader.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + msg);
		}
	}

	public boolean readConfig() {
		// Load LanguageReader
		languageReader = new LanguageReader(new File(breweryDriver.getDataFolder(), "en.yml"));
		
		/*** config.yml ***/
		File currentFile = new File(breweryDriver.getDataFolder(), "config.yml");
		if (!checkConfigs()) {
			return false;
		}
		FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(currentFile);

		// various Settings
		DataSave.autosave = currentConfig.getInt("autosave", 3);
		debug = currentConfig.getBoolean("debug", false);
		BPlayer.pukeItem = Material.matchMaterial(currentConfig.getString("pukeItem", "SOUL_SAND"));
		BPlayer.hangoverTime = currentConfig.getInt("hangoverDays", 0) * 24 * 60;
		BPlayer.overdrinkKick = currentConfig.getBoolean("enableKickOnOverdrink", false);
		BPlayer.enableHome = currentConfig.getBoolean("enableHome", false);
		BPlayer.enableLoginDisallow = currentConfig.getBoolean("enableLoginDisallow", false);
		BPlayer.enablePuke = currentConfig.getBoolean("enablePuke", false);
		BPlayer.pukeDespawntime = currentConfig.getInt("pukeDespawntime", 60) * 20;
		BPlayer.homeType = currentConfig.getString("homeType", null);
		Brew.colorInBarrels = currentConfig.getBoolean("colorInBarrels", false);
		Brew.colorInBrewer = currentConfig.getBoolean("colorInBrewer", false);
		PlayerListener.openEverywhere = currentConfig.getBoolean("openLargeBarrelEverywhere", false);
		
		
		/*** parameters.yml ***/
		currentFile = new File(breweryDriver.getDataFolder(), "parameters.yml");
		if(!currentFile.exists()) {
			return false;
		}
		currentConfig = YamlConfiguration.loadConfiguration(currentFile);
		
		//difficulty settings
		Barrel.minutesPerYear = currentConfig.getDouble("minutesPerYear", 20.0);
		
		// loading drainItems
		List<String> drainList = currentConfig.getStringList("drainItems");
		if (drainList != null) {
			for (String drainString : drainList) {
				String[] drainSplit = drainString.split("/");
				if (drainSplit.length > 1) {
					Material mat = Material.matchMaterial(drainSplit[0]);
					int strength = breweryDriver.parseInt(drainSplit[1]);
					if (mat != null && strength > 0) {
						BPlayer.drainItems.put(mat, strength);
					}
				}
			}
		}
		
		
		/*** recipes.yml ***/
		currentFile = new File(breweryDriver.getDataFolder(), "recipes.yml");
		if(!currentFile.exists()) {
			return false;
		}
		currentConfig = YamlConfiguration.loadConfiguration(currentFile);
		
		// loading recipes
		ConfigurationSection configSection = currentConfig.getConfigurationSection("recipes");
		if (configSection != null) {
			for (String recipeId : configSection.getKeys(false)) {
				BRecipe recipe = new BRecipe(configSection, recipeId);
				if (recipe.isValid()) {
					BRecipe.recipes.add(recipe);
				} else {
					errorLog("Loading the Recipe with id: '" + recipeId + "' failed!");
				}
			}
		}
		
		/*** words.yml ***/
		currentFile = new File(breweryDriver.getDataFolder(), "words.yml");
		if(!currentFile.exists()) {
			return false;
		}
		currentConfig = YamlConfiguration.loadConfiguration(currentFile);
		// Loading Words
		if (currentConfig.getBoolean("enableChatDistortion", false)) {
			for (Map<?, ?> map : currentConfig.getMapList("words")) {
				new Words(map);
			}
			for (String bypass : currentConfig.getStringList("distortBypass")) {
				Words.ignoreText.add(bypass.split(","));
			}
			Words.commands = currentConfig.getStringList("distortCommands");
		}
		Words.log = currentConfig.getBoolean("logRealChat", false);
		Words.doSigns = currentConfig.getBoolean("distortSignText", false);
		
		
		/*** aspects.yml ***/
		currentFile = new File(breweryDriver.getDataFolder(), "aspects.yml");
		if(!currentFile.exists()) {
			return false;
		}
		currentConfig = YamlConfiguration.loadConfiguration(currentFile);
		
		//type map
		configSection = currentConfig.getConfigurationSection("types");
		if(configSection != null) {
			for (String types: configSection.getKeys(false)) {
				List<String> ingredient = configSection.getStringList(types);
				for(String type : ingredient) {
					debugLog("added: " + type + " makes a " + types);
					BIngredients.typeMap.put(type, types);
				}
			}
		}

		return true;
		
		//TODO: Aspects
		
	}

	// load all Data
	public void readData() {
		File file = new File(breweryDriver.getDataFolder(), "data.yml");
		if (file.exists()) {

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			Brew.installTime = data.getLong("installTime", System.currentTimeMillis());

			// Check if data is the newest version
			String version = data.getString("Version", null);
			if (version != null) {
				if (!version.equals(DataSave.dataVersion)) {
					Brewery.breweryDriver.log("Data File is being updated...");
					new DataUpdater(data, file).update(version);
					data = YamlConfiguration.loadConfiguration(file);
					Brewery.breweryDriver.log("Data Updated to version: " + DataSave.dataVersion);
				}
			}

			// loading Ingredients into ingMap
			Map<String, BIngredients> ingMap = new HashMap<String, BIngredients>();
			ConfigurationSection section = data.getConfigurationSection("Ingredients");
			if (section != null) {
				for (String id : section.getKeys(false)) {
					ConfigurationSection matSection = section.getConfigurationSection(id + ".mats");
					if (matSection != null) {
						// matSection has all the materials + amount as Integers
						ArrayList<ItemStack> ingredients = deserializeIngredients(matSection);
						ingMap.put(id, new BIngredients(ingredients, section.getInt(id + ".cookedTime", 0)));
					} else {
						errorLog("Ingredient id: '" + id + "' incomplete in data.yml");
					}
				}
			}

			// loading Brew
			section = data.getConfigurationSection("Brew");
			if (section != null) {
				// All sections have the UID as name
				for (String uid : section.getKeys(false)) {
					BIngredients ingredients = getIngredients(ingMap, section.getString(uid + ".ingId"));
					int quality = section.getInt(uid + ".quality", 0);
					int distillRuns = section.getInt(uid + ".distillRuns", 0);
					float ageTime = (float) section.getDouble(uid + ".ageTime", 0.0);
					float wood = (float) section.getDouble(uid + ".wood", -1.0);
					String recipe = section.getString(uid + ".recipe", null);
					boolean unlabeled = section.getBoolean(uid + ".unlabeled", false);
					boolean persistent = section.getBoolean(uid + ".persist", false);
					boolean stat = section.getBoolean(uid + ".stat", false);
					int lastUpdate = section.getInt("lastUpdate", 0);

					new Brew(parseInt(uid), ingredients, quality, distillRuns, ageTime, wood, recipe, unlabeled, persistent, stat, lastUpdate);
				}
			}

			// loading BPlayer
			section = data.getConfigurationSection("Player");
			if (section != null) {
				// keys have players name
				for (String name : section.getKeys(false)) {
					try {
						//noinspection ResultOfMethodCallIgnored
						UUID.fromString(name);
						if (!useUUID) {
							continue;
						}
					} catch (IllegalArgumentException e) {
						if (useUUID) {
							continue;
						}
					}

					int quality = section.getInt(name + ".quality");
					int drunk = section.getInt(name + ".drunk");
					int offDrunk = section.getInt(name + ".offDrunk", 0);

					new BPlayer(name, quality, drunk, offDrunk);
				}
			}

			for (World world : breweryDriver.getServer().getWorlds()) {
				if (world.getName().startsWith("DXL_")) {
					loadWorldData(getDxlName(world.getName()), world);
				} else {
					loadWorldData(world.getUID().toString(), world);
				}
			}

		} else {
			errorLog("No data.yml found, will create new one!");
		}
	}

	public ArrayList<ItemStack> deserializeIngredients(ConfigurationSection matSection) {
		ArrayList<ItemStack> ingredients = new ArrayList<ItemStack>();
		for (String mat : matSection.getKeys(false)) {
			String[] matSplit = mat.split(",");
			ItemStack item = new ItemStack(Material.getMaterial(matSplit[0]), matSection.getInt(mat));
			if (matSplit.length == 2) {
				item.setDurability((short) Brewery.breweryDriver.parseInt(matSplit[1]));
			}
			ingredients.add(item);
		}
		return ingredients;
	}

	// returns Ingredients by id from the specified ingMap
	public BIngredients getIngredients(Map<String, BIngredients> ingMap, String id) {
		if (!ingMap.isEmpty()) {
			if (ingMap.containsKey(id)) {
				return ingMap.get(id);
			}
		}
		errorLog("Ingredient id: '" + id + "' not found in data.yml");
		return new BIngredients();
	}

	// loads BIngredients from an ingredient section
	public BIngredients loadIngredients(ConfigurationSection section) {
		if (section != null) {
			return new BIngredients(deserializeIngredients(section), 0);
		} else {
			errorLog("Cauldron is missing Ingredient Section");
		}
		return new BIngredients();
	}

	// load Block locations of given world
	public void loadWorldData(String uuid, World world) {

		File file = new File(breweryDriver.getDataFolder(), "data.yml");
		if (file.exists()) {

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			// loading BCauldron
			if (data.contains("BCauldron." + uuid)) {
				ConfigurationSection section = data.getConfigurationSection("BCauldron." + uuid);
				for (String cauldron : section.getKeys(false)) {
					// block is splitted into x/y/z
					String block = section.getString(cauldron + ".block");
					if (block != null) {
						String[] splitted = block.split("/");
						if (splitted.length == 3) {

							Block worldBlock = world.getBlockAt(parseInt(splitted[0]), parseInt(splitted[1]), parseInt(splitted[2]));
							BIngredients ingredients = loadIngredients(section.getConfigurationSection(cauldron + ".ingredients"));
							int state = section.getInt(cauldron + ".state", 1);

							new BCauldron(worldBlock, ingredients, state);
						} else {
							errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
						}
					} else {
						errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
					}
				}
			}

			// loading Barrel
			if (data.contains("Barrel." + uuid)) {
				ConfigurationSection section = data.getConfigurationSection("Barrel." + uuid);
				for (String barrel : section.getKeys(false)) {
					// block spigot is splitted into x/y/z
					String spigot = section.getString(barrel + ".spigot");
					if (spigot != null) {
						String[] splitted = spigot.split("/");
						if (splitted.length == 3) {

							// load itemStacks from invSection
							ConfigurationSection invSection = section.getConfigurationSection(barrel + ".inv");
							Block block = world.getBlockAt(parseInt(splitted[0]), parseInt(splitted[1]), parseInt(splitted[2]));
							float time = (float) section.getDouble(barrel + ".time", 0.0);
							byte sign = (byte) section.getInt(barrel + ".sign", 0);
							String[] st = section.getString(barrel + ".st", "").split(",");
							String[] wo = section.getString(barrel + ".wo", "").split(",");

							if (invSection != null) {
								new Barrel(block, sign, st, wo, invSection.getValues(true), time);
							} else {
								// Barrel has no inventory
								new Barrel(block, sign, st, wo, null, time);
							}

						} else {
							errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
						}
					} else {
						errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
					}
				}
			}

			// loading Wakeup
			if (data.contains("Wakeup." + uuid)) {
				ConfigurationSection section = data.getConfigurationSection("Wakeup." + uuid);
				for (String wakeup : section.getKeys(false)) {
					// loc of wakeup is splitted into x/y/z/pitch/yaw
					String loc = section.getString(wakeup);
					if (loc != null) {
						String[] splitted = loc.split("/");
						if (splitted.length == 5) {

							double x = NumberUtils.toDouble(splitted[0]);
							double y = NumberUtils.toDouble(splitted[1]);
							double z = NumberUtils.toDouble(splitted[2]);
							float pitch = NumberUtils.toFloat(splitted[3]);
							float yaw = NumberUtils.toFloat(splitted[4]);
							Location location = new Location(world, x, y, z, yaw, pitch);

							Wakeup.wakeups.add(new Wakeup(location));

						} else {
							errorLog("Incomplete Location-Data in data.yml: " + section.getCurrentPath() + "." + wakeup);
						}
					}
				}
			}

		}
	}

	private boolean checkConfigs() {
		File cfg = new File(breweryDriver.getDataFolder(), "config.yml");
		if (!cfg.exists()) {
			errorLog("No config.yml found, creating default file! You may want to choose a config according to your language!");
			errorLog("You can find them in plugins/Brewery/configs/");
			InputStream defconf = getResource("config/en/config.yml");
			if (defconf == null) {
				errorLog("default config file not found, your jarfile may be corrupt. Disabling Brewery!");
				return false;
			}
			try {
				saveFile(defconf, getDataFolder(), "config.yml", false);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		if (!cfg.exists()) {
			errorLog("default config file could not be copied, your jarfile may be corrupt. Disabling Brewery!");
			return false;
		}

		copyDefaultConfigs(false);
		return true;
	}

	private void copyDefaultConfigs(boolean overwrite) {
		File configs = new File(getDataFolder(), "configs");
		File languages = new File(getDataFolder(), "languages");
		for (String l : new String[] {"de", "en", "fr", "it"}) {
			File lfold = new File(configs, l);
			try {
				saveFile(getResource("config/" + l + "/config.yml"), lfold, "config.yml", overwrite);
				saveFile(getResource("languages/" + l + ".yml"), languages, l + ".yml", false); // Never overwrite languages for now
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Utility

	public int parseInt(String string) {
		return NumberUtils.toInt(string, 0);
	}

	// gets the Name of a DXL World
	public String getDxlName(String worldName) {
		File dungeonFolder = new File(worldName);
		if (dungeonFolder.isDirectory()) {
			for (File file : dungeonFolder.listFiles()) {
				if (!file.isDirectory()) {
					if (file.getName().startsWith(".id_")) {
						return file.getName().substring(1).toLowerCase();
					}
				}
			}
		}
		return worldName;
	}

	// create empty World save Sections
	public void createWorldSections(ConfigurationSection section) {
		for (World world : breweryDriver.getServer().getWorlds()) {
			String worldName = world.getName();
			if (worldName.startsWith("DXL_")) {
				worldName = getDxlName(worldName);
			} else {
				worldName = world.getUID().toString();
			}
			section.createSection(worldName);
		}
	}

	// prints a list of Strings at the specified page
	public void list(CommandSender sender, ArrayList<String> strings, int page) {
		int pages = (int) Math.ceil(strings.size() / 7F);
		if (page > pages || page < 1) {
			page = 1;
		}

		sender.sendMessage(color("&7-------------- &f" + languageReader.get("Etc_Page") + " &6" + page + "&f/&6" + pages + " &7--------------"));

		ListIterator<String> iter = strings.listIterator((page - 1) * 7);

		for (int i = 0; i < 7; i++) {
			if (iter.hasNext()) {
				sender.sendMessage(color(iter.next()));
			} else {
				break;
			}
		}
	}

	// Returns true if the Block can be destroyed by the Player or something else (null)
	public boolean blockDestroy(Block block, Player player) {
		switch (block.getType()) {
		case CAULDRON:
			// will only remove when existing
			BCauldron.remove(block);
			return true;
		case OAK_FENCE:
		case NETHER_BRICK_FENCE:
		case ACACIA_FENCE:
		case BIRCH_FENCE:
		case DARK_OAK_FENCE:
		case IRON_BARS:
		case JUNGLE_FENCE:
		case SPRUCE_FENCE:
			// remove barrel and throw potions on the ground
			Barrel barrel = Barrel.getBySpigot(block);
			if (barrel != null) {
				barrel.remove(null, player);
			}
			return true;
		case SIGN:
		case WALL_SIGN:
			// remove small Barrels
			Barrel barrel2 = Barrel.getBySpigot(block);
			if (barrel2 != null) {
				if (!barrel2.isLarge()) {
						barrel2.remove(null, player);
				} else {
					barrel2.destroySign();
				}
			}
			return true;
		case OAK_PLANKS:
		case OAK_STAIRS:
		case BIRCH_STAIRS:
		case JUNGLE_STAIRS:
		case SPRUCE_STAIRS:
		case ACACIA_STAIRS:
		case DARK_OAK_STAIRS:
			Barrel barrel3 = Barrel.getByWood(block);
			if (barrel3 != null) {
					barrel3.remove(block, player);
			}
		default:
			break;
		}
		return true;
	}

	public String color(String msg) {
		if (msg != null) {
			msg = ChatColor.translateAlternateColorCodes('&', msg);
		}
		return msg;
	}

	public static void saveFile(InputStream in, File dest, String name, boolean overwrite) throws IOException {
		if (in == null) return;
		if (!dest.exists()) {
			dest.mkdirs();
		}
		File result = new File(dest, name);
		if (result.exists()) {
			if (overwrite) {
				result.delete();
			} else {
				return;
			}
		}

		OutputStream out = new FileOutputStream(result);
		byte[] buffer = new byte[1024];

		int length;
		//copy the file content in bytes
		while ((length = in.read(buffer)) > 0){
			out.write(buffer, 0, length);
		}

		in.close();
		out.close();
	}

	// Returns either uuid or Name of player, depending on bukkit version
	public static String playerString(Player player) {
		if (useUUID) {
			return player.getUniqueId().toString();
		} else {
			return player.getName();
		}
	}

	// returns the Player if online
	public static Player getPlayerfromString(String name) {
	return Bukkit.getPlayer(UUID.fromString(name));
	}

	// Runnables

	public class DrunkRunnable implements Runnable {
		@Override
		public void run() {
			if (!BPlayer.isEmpty()) {
				BPlayer.drunkeness();
			}
		}
	}

	public class BreweryRunnable implements Runnable {
		@Override
		public void run() {
			reloader = null;
			for (BCauldron cauldron : BCauldron.bcauldrons) {
				cauldron.onUpdate();// runs every min to update cooking time
			}
			Barrel.onUpdate();// runs every min to check and update ageing time
			BPlayer.onUpdate();// updates players drunkeness

			debugLog("Update");

			DataSave.autoSave();
		}

	}

}
