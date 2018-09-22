package com.dreamless.brewery.filedata;


import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.dreamless.brewery.Brewery;

public class ReadOldData extends BukkitRunnable {

	public FileConfiguration data;
	public boolean done = false;

	@Override
	public void run() {
		File datafile = new File(Brewery.breweryDriver.getDataFolder(), "data.yml");
		data = YamlConfiguration.loadConfiguration(datafile);

		if (DataSave.lastBackup > 10) {
			datafile.renameTo(new File(Brewery.breweryDriver.getDataFolder(), "dataBackup.yml"));
			DataSave.lastBackup = 0;
		} else {
			DataSave.lastBackup++;
		}

		done = true;
	}

	public FileConfiguration getData() {
		return data;
	}

}
