package com.dreamless.brewery.brew;

import java.io.FileReader;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import com.dreamless.brewery.Brewery;
import com.opencsv.CSVReader;

public class IngredientDatabase {

	private static HashMap<Material, IngredientData> ingredientDatabase = new HashMap<Material, IngredientData>();
	
	public static Boolean readConfig(String file)
	{	
		try
		{
			//FileReader fileReader = new FileReader(file);
			
			CSVReader csvReader = new CSVReader(new FileReader(file));
			String[] nextLine;
			
			while((nextLine = csvReader.readNext()) != null)
			{
				if(nextLine.length != 6)
				{
					Brewery.breweryDriver.errorLog("Unable to add ingredient: " + nextLine[0]);
					continue;
				}
				IngredientData data = new IngredientData(
						nextLine[1], 
						nextLine[2],
						PotionEffectType.getByName(nextLine[3]),
						nextLine[4],
						Rarity.valueOf(nextLine[5]));
				ingredientDatabase.put(Material.getMaterial(nextLine[0]), data);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static void clear()
	{
		ingredientDatabase.clear();
	}
}
