package com.dreamless.brewery.filedata;


import org.bukkit.scheduler.BukkitRunnable;

import com.dreamless.brewery.BCauldron;
import com.dreamless.brewery.BPlayer;
import com.dreamless.brewery.Barrel;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.Wakeup;

public class DataSave extends BukkitRunnable {

	public static int lastBackup = 0;
	public static int lastSave = 1;
	public static int autosave = 3;
	final public static String dataVersion = "1.1";
	public static DataSave running;

	public boolean collected = false;

	// Not Thread-Safe! Needs to be run in main thread but uses async Read/Write
	public DataSave() {
		System.currentTimeMillis();
	}


	@Override
	public void run() {
		Brewery.breweryDriver.debugLog("Starting save...");
		if (Brewery.loadcauldrons) {
			BCauldron.save();
		} else {
			Brewery.breweryDriver.debugLog("Cauldron saving disabled");
		}
		//Brewery.breweryDriver.debugLog("CAUL SAVE");
		if (Brewery.loadbarrels) {
			Barrel.save();
		} else {
			Brewery.breweryDriver.debugLog("Barrel saving disabled");
		}
		//Brewery.breweryDriver.debugLog("BAR SAVE");
		if (!BPlayer.isEmpty() && Brewery.loadplayers) {
			BPlayer.save();
		} else {
			Brewery.breweryDriver.debugLog("Player saving disabled");
		}
		//Brewery.breweryDriver.debugLog("P SAVE");
		if (!Wakeup.wakeups.isEmpty() && Brewery.loadwakeup) {
			Wakeup.save();
		} else {
			Brewery.breweryDriver.debugLog("Wakeup saving disabled");
		}
		//Brewery.breweryDriver.debugLog("W SAVE");
	}

	// Finish the collection of data immediately



	// Save all data. Takes a boolean whether all data should be collected in instantly
	public static void save() {
		
		//Do not save if in dev mode
		/*if(Brewery.development) {
			Brewery.breweryDriver.debugLog("Saving disabled in development mode");
			return;
		}*/
		
		long time = System.nanoTime();
		if (running != null) {
			Brewery.breweryDriver.log("Another Save was started while a Save was in Progress");
			return;
		}

		running = new DataSave();
		Brewery.breweryDriver.debugLog("Beginning to save...");
		running.run();
		Brewery.breweryDriver.debugLog("saving: " + ((System.nanoTime() - time) / 1000000.0) + "ms");
		running = null;
		lastSave = 1;
	}

	public static void autoSave() {
		if (lastSave >= autosave) {
			save();// save all data
		} else {
			lastSave++;
		}
	}
}
