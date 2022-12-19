package com.dreamless.brewery.brew;

import java.io.FileReader;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import com.dreamless.brewery.Brewery;
import com.opencsv.CSVReader;

public class IngredientDatabase {

	private static HashMap<Material, IngredientData> ingredientDatabase = new HashMap<Material, IngredientData>();
	private static final Material ALCOHOLIC_MATERIAL = Material.PHANTOM_MEMBRANE; // TODO: Make configurable
	
	public static Boolean readConfig(String file)
	{	
		try
		{
			CSVReader csvReader = new CSVReader(new FileReader(file));
			String[] nextLine;
			
			while((nextLine = csvReader.readNext()) != null)
			{
				if(nextLine.length != 9)
				{
					Brewery.breweryDriver.errorLog("Unable to add ingredient: " + nextLine[0]);
					continue;
				}
				IngredientData data = new IngredientData(
						nextLine[1], // Name
						nextLine[2], // Alcoholic Name
						PotionEffectType.getByName(nextLine[3]), // Effect
						nextLine[4], // flavor
						Rarity.valueOf(nextLine[5]), // Rarity
						Integer.parseInt(nextLine[6]), // Red
						Integer.parseInt(nextLine[7]), // Green
						Integer.parseInt(nextLine[8])); // Blue
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
	
	public static boolean isIngredient(Material material)
	{
		return ingredientDatabase.containsKey(material);
	}
	
	public static Material getAlcoholicIngredient()
	{
		return ALCOHOLIC_MATERIAL;
	}
	
	public static IngredientData getIngredientData(Material material)
	{
		return ingredientDatabase.get(material);
	}
}
