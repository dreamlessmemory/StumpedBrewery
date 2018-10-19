package com.dreamless.brewery.filedata;


import java.io.File;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.dreamless.brewery.BCauldron;
import com.dreamless.brewery.BPlayer;
import com.dreamless.brewery.Barrel;
import com.dreamless.brewery.Brew;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.Wakeup;

public class DataSave extends BukkitRunnable {

	public static int lastBackup = 0;
	public static int lastSave = 1;
	public static int autosave = 3;
	final public static String dataVersion = "1.1";
	public static DataSave running;

	public ReadOldData read;
	private long time;
	public boolean collected = false;

	// Not Thread-Safe! Needs to be run in main thread but uses async Read/Write
	public DataSave(ReadOldData read) {
		this.read = read;
		time = System.currentTimeMillis();
	}


	@Override
	public void run() {
		if (read != null) {
			if (!read.done) {
				// Wait for async thread to load old data
				if (System.currentTimeMillis() - time > 30000) {
					Brewery.breweryDriver.errorLog("Old Data took too long to load!");
					cancel();
					return;
				}
				return;
			}
		} 
		try {
			cancel();
		} catch (IllegalStateException ignored) {
		}

		FileConfiguration configFile = new YamlConfiguration();

		configFile.set("installTime", Brew.installTime);
		
		//TODO: Convert to SQL
		if (!BCauldron.bcauldrons.isEmpty()) {
			BCauldron.save();
		}
		
		//TODO: Convert to SQL
		if (!Barrel.barrels.isEmpty()) {
			Barrel.save();
		}
		
		//TODO: Convert to SQL
		if (!BPlayer.isEmpty()) {
			BPlayer.save();
		}

		//TODO: Convert to SQL
		if (!Wakeup.wakeups.isEmpty()) {
			Wakeup.save();
		}

		//saveWorldNames(configFile, oldData.getConfigurationSection("Worlds"));
		configFile.set("Version", dataVersion);

		collected = true;
		if (Brewery.breweryDriver.isEnabled()) {
			Brewery.breweryDriver.getServer().getScheduler().runTaskAsynchronously(Brewery.breweryDriver, new WriteData(configFile));
		} else {
			new WriteData(configFile).run();
		}
	}

	// Finish the collection of data immediately
	public void now() {
		if (!read.done) {
			read.cancel();
			read.run();
		}
		if (!collected) {
			cancel();
			run();
		}
	}



	// Save all data. Takes a boolean whether all data should be collected in instantly
	public static void save(boolean collectInstant) {
		long time = System.nanoTime();
		if (running != null) {
			Brewery.breweryDriver.log("Another Save was started while a Save was in Progress");
			if (collectInstant) {
				running.now();
			}
			return;
		}
		File datafile = new File(Brewery.breweryDriver.getDataFolder(), "data.yml");

		if (datafile.exists()) {
			ReadOldData read = new ReadOldData();
			if (collectInstant) {
				read.run();
				running = new DataSave(read);
				running.run();
			} else {
				read.runTaskAsynchronously(Brewery.breweryDriver);
				running = new DataSave(read);
				running.runTaskTimer(Brewery.breweryDriver, 1, 2);
			}
		} else {
			running = new DataSave(null);
			running.run();
		}
		Brewery.breweryDriver.debugLog("saving: " + ((System.nanoTime() - time) / 1000000.0) + "ms");
	}

	public static void autoSave() {
		if (lastSave >= autosave) {
			save(false);// save all data
		} else {
			lastSave++;
		}
	}

	public static void saveWorldNames(FileConfiguration root, ConfigurationSection old) {
		if (old != null) {
			root.set("Worlds", old);
		}
		for (World world : Brewery.breweryDriver.getServer().getWorlds()) {
			String worldName = world.getName();
			if (worldName.startsWith("DXL_")) {
				worldName = Brewery.breweryDriver.getDxlName(worldName);
				root.set("Worlds." + worldName, 0);
			} else {
				worldName = world.getUID().toString();
				root.set("Worlds." + worldName, world.getName());
			}
		}
	}
}
