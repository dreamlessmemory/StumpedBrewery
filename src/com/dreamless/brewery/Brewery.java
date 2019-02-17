package com.dreamless.brewery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.dreamless.brewery.filedata.*;
import com.dreamless.brewery.listeners.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;

public class Brewery extends JavaPlugin {
	//Params
	public static Brewery breweryDriver;
	public static boolean useUUID;
	public static boolean use1_9;
	public static boolean updateCheck;
	
	//debug
	public static boolean debug;
	public static boolean development;
	public static boolean loadcauldrons;
	public static boolean loadbarrels;
	public static boolean loadwakeup;
	public static boolean loadplayers;
	public static boolean newrecipes;

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
	private String username; 
	private String password; 
	private String url;
	public static String database;

	//Connection vars
	public static Connection connection; //This is the variable we will use to connect to database
	public static Gson gson;

	@Override
	public void onEnable() {
		breweryDriver = this;

		// Version check
		String v = Bukkit.getBukkitVersion();
		useUUID = !v.matches("(^|.*[^\\.\\d])1\\.[0-6]([^\\d].*|$)") && !v.matches("(^|.*[^\\.\\d])1\\.7\\.[0-5]([^\\d].*|$)");
		use1_9 = !v.matches("(^|.*[^\\.\\d])1\\.[0-8]([^\\d].*|$)");
		

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
		
		//Load the GSON
		gson = new Gson();
		
		//Do not load anything if in dev mode
		if(!development) {
			readData();
		} else {
			Brewery.breweryDriver.debugLog("Loading disabled in development mode");
		}

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
		breweryDriver.getServer().getScheduler().runTaskTimer(breweryDriver, new CauldronRunnable(), 120, 20);
		breweryDriver.getServer().getScheduler().runTaskTimer(breweryDriver, new DrunkRunnable(), 120, 120);
		breweryDriver.getServer().getScheduler().runTaskTimer(breweryDriver, new RecipeRunnable(), 650, 216000);//3 hours = 216000

		this.log(this.getDescription().getName() + " enabled!");
	}

	@Override
	public void onDisable() {
		

		// Disable listeners
		HandlerList.unregisterAll(this);

		// Stop shedulers
		getServer().getScheduler().cancelTasks(this);

		if (breweryDriver == null) {
			return;
		}

		// save Data to Disk
		DataSave.save();

		// save LanguageReader
		languageReader.save();

		// delete Data from Ram
		Barrel.barrels.clear();
		BCauldron.bcauldrons.clear();
		//BRecipe.recipes.clear();
		//BIngredients.cookedNames.clear();
		BPlayer.clear();
		Wakeup.wakeups.clear();
		Words.words.clear();
		Words.ignoreText.clear();
		Words.commands = null;

		// Disable Server
	    try { //using a try catch to catch connection errors (like wrong sql password...)
	        if (connection!=null && !connection.isClosed()){ //checking if connection isn't null to
	            //avoid receiving a nullpointer
	            connection.close(); //closing the connection field variable.
	        }
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
		
		this.log(this.getDescription().getName() + " disabled!");
	}

	public void reload(CommandSender sender) {
		if (sender != null && !sender.equals(getServer().getConsoleSender())) {
			reloader = sender;
		}
		// clear all existent config Data
		//BRecipe.recipes.clear();
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
		languageReader = new LanguageReader(new File(breweryDriver.getDataFolder(), "en.yml"));

		reloader = null;
	}

	public void msg(CommandSender sender, String msg) {
		sender.sendMessage(color("&2[Brewery] &f" + msg));
	}
	
	public void msg(CommandSender sender, ArrayList<String> msg) {
		sender.sendMessage(msg.toArray(new String[msg.size()]));
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
		
		//Database seetings
		username= currentConfig.getString("username");
		password = currentConfig.getString("password");
		url = currentConfig.getString("url");
		database = currentConfig.getString("prefix");

		// various Settings
		DataSave.autosave = currentConfig.getInt("autosave", 3);
		debug = currentConfig.getBoolean("debug", false);
		development = currentConfig.getBoolean("development", false);
		loadcauldrons = currentConfig.getBoolean("loadcauldrons", true);
		loadbarrels = currentConfig.getBoolean("loadbarrels", true);
		loadwakeup = currentConfig.getBoolean("loadwakeup", true);
		loadplayers = currentConfig.getBoolean("loadplayers", true);
		newrecipes = currentConfig.getBoolean("newrecipes", true);
		BPlayer.pukeItem = Material.matchMaterial(currentConfig.getString("pukeItem", "SOUL_SAND"));
		BPlayer.hangoverTime = currentConfig.getInt("hangoverDays", 0) * 24 * 60;
		BPlayer.overdrinkKick = currentConfig.getBoolean("enableKickOnOverdrink", false);
		BPlayer.enableHome = currentConfig.getBoolean("enableHome", false);
		BPlayer.enableLoginDisallow = currentConfig.getBoolean("enableLoginDisallow", false);
		BPlayer.enablePuke = currentConfig.getBoolean("enablePuke", false);
		BPlayer.pukeDespawntime = currentConfig.getInt("pukeDespawntime", 60) * 20;
		BPlayer.homeType = currentConfig.getString("homeType", null);
		PlayerListener.openEverywhere = currentConfig.getBoolean("openLargeBarrelEverywhere", false);
		
		//difficulty settings
		Barrel.minutesPerYear = currentConfig.getDouble("minutesPerYear", 10.0);
		
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
		

		return true;
		
	}

	// load all Data
	public void readData() {
		//Brew.installTime = data.getLong("installTime", System.currentTimeMillis());

		//Cauldrons
		if(loadcauldrons) {
			loadCauldrons();
			Brewery.breweryDriver.debugLog("Cauldrons loaded");
		} else {
			Brewery.breweryDriver.debugLog("Cauldron loading disabled");
		}
		
		//Barrel
		if(loadbarrels) {
			loadBarrels();
			Brewery.breweryDriver.debugLog("Barrels loaded");
		} else {
			Brewery.breweryDriver.debugLog("Barrel loading disabled");
		}
			
		//Player
		if(loadplayers) {
			loadPlayers();
			Brewery.breweryDriver.debugLog("Players loaded");
		} else {
			Brewery.breweryDriver.debugLog("Player loading disabled");
		}
		
		//Wakeup
		if(loadwakeup) {
			loadWakeup();
			Brewery.breweryDriver.debugLog("Wakeups loaded");
		} else {
			Brewery.breweryDriver.debugLog("Wakeup loading disabled");
		}
	}
	
	private void loadCauldrons() {
		//TODO: revert cauldron_test -> cauldrons
		String cauldronQuery = "SELECT * FROM " + Brewery.database + "cauldron_test";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(cauldronQuery)){					
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				//Block
				HashMap<String, Object> locationMap = Brewery.gson.fromJson(result.getString("location"), new TypeToken<HashMap<String, Object>>(){}.getType());
				debugLog(locationMap.toString());
				Block worldBlock = (Location.deserialize(locationMap).getBlock());
				debugLog(worldBlock.toString());
				if(((Levelled)worldBlock.getBlockData()).getLevel() < 1) {
					debugLog("Skipping loading, empty");
					continue;
				}
				//State
				int state = result.getInt("state");
				
				//Cooked
				boolean cooking = result.getBoolean("cooking");
				
				//Ingredients
				String inventory = result.getString("contents");
				HashMap<String, Aspect> aspects = Brewery.gson.fromJson(result.getString("aspects"), new TypeToken<HashMap<String, Aspect>>(){}.getType());
				BIngredients ingredients = new BIngredients (inventory, aspects, state, cooking);
				
				//lastCook
				long lastCook = result.getLong("lastCook");
							
				new BCauldron(worldBlock, ingredients, state, cooking, lastCook);
			} 
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private void loadBarrels() {
		String barrelQuery = "SELECT * FROM " + Brewery.database + "barrels";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(barrelQuery)){						
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				String resultString;
				//spigot
				HashMap<String, Object> locationMap = gson.fromJson(result.getString("location"), new TypeToken<HashMap<String, Object>>(){}.getType());
				debugLog(locationMap.toString());
				Block worldBlock = (Location.deserialize(locationMap).getBlock());
				debugLog(worldBlock.toString());
				
				//Wood
				int[] woodsLoc = null;
				resultString = result.getString("woodsloc");
				if(resultString != null) {
					woodsLoc = gson.fromJson(resultString, int[].class);
				}
				
				//Stairs
				int[] stairsLoc = null;
				resultString = result.getString("stairsloc");
				if(resultString != null) {
					stairsLoc = gson.fromJson(resultString, int[].class);
				}
			
				//Sign
				byte signoffset = result.getByte("signoffset");
			
				//Inventory
				String inventory = result.getString("inventory");

				//Time
				float time = result.getFloat("time");
				
				new Barrel(worldBlock, signoffset, woodsLoc, stairsLoc, inventory, time);
			} 
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	private void loadPlayers() {
		String playerQuery = "SELECT * FROM " + Brewery.database + "players";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(playerQuery)){						
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				int drunkeness = result.getInt("drunkeness");
				int offDrunk = result.getInt("offlinedrunk");
				if(drunkeness > 0 || offDrunk > 0) {
					new BPlayer(result.getString("uuid"), drunkeness, offDrunk, result.getBoolean("drunkeffects"));
					//debugLog(result.getString("uuid"));
				}
			} 
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	private void loadWakeup() {
		String wakeupQuery = "SELECT location FROM " + Brewery.database + "wakeup";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(wakeupQuery)){						
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				HashMap<String, Object> locationMap = gson.fromJson(result.getString("location"), new TypeToken<HashMap<String, Object>>(){}.getType());
				//debugLog("Wakeup : " + locationMap.toString());
				Wakeup.wakeups.add(new Wakeup(Location.deserialize(locationMap)));
			} 
		} catch (Exception e1) {
			e1.printStackTrace();
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
	
	//get a UUID from a name
	public UUID getUUID(String name) throws ParseException, org.json.simple.parser.ParseException {
        String url = "https://api.mojang.com/users/profiles/minecraft/"+name;
        try {
            String UUIDJson = IOUtils.toString(new URL(url), "US-ASCII");
            if(UUIDJson.isEmpty()) {
            	return null;
            }
            JSONObject UUIDObject = (JSONObject) JSONValue.parseWithException(UUIDJson);       
            String tempID = UUIDObject.get("id").toString();
            tempID = tempID.substring(0,  8) + "-" + tempID.substring(8,  12) + "-" + tempID.substring(12,  16) + "-" + tempID.substring(16,  20) + "-" + tempID.substring(20);
            return UUID.fromString(tempID);
        } catch (IOException e) {
            e.printStackTrace();
        }       
        return null;
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
			Barrel.onUpdate();// runs every min to check and update ageing time
			BPlayer.onUpdate();// updates players drunkeness

			debugLog("Update");

			DataSave.autoSave();
		}

	}
	
	public class CauldronRunnable implements Runnable {
		@Override
		public void run() {
			//reloader = null;
			for (BCauldron cauldron : BCauldron.bcauldrons) {
				cauldron.onUpdate();// runs every second
			}
		}

	}
	
	public class RecipeRunnable implements Runnable {
		@Override
		public void run() {
			BRecipe.periodicPurge();
		}
	}
}
