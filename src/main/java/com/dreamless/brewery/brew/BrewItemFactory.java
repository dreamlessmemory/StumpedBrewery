package com.dreamless.brewery.brew;

import java.text.ParseException;
import java.util.ArrayList;
import java.lang.String;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dreamless.brewery.data.DatabaseCommunication;
import com.dreamless.brewery.data.NBTConstants;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BrewItemFactory {

	public static ItemStack getAgedBrew(MashBucket mashBucket, int age, BarrelType type) 
	{
		// Error case - somehow the barrel got emptied
		if(mashBucket == null)
		{
			return getRuinedBrew();
		}

		// Primary Ingredient
		IngredientData primaryIngredientData = IngredientDatabase.getIngredientData(mashBucket.getPrimaryIngredient().getType());

		// Secondary Ingredient
		IngredientData secondaryIngredientData = mashBucket.getSecondaryIngredient() == null ? 
				null :  IngredientDatabase.getIngredientData(mashBucket.getSecondaryIngredient().getType());
		IngredientData flavorIngredientData = mashBucket.getFlavorIngredient() == null ? 
				null :  IngredientDatabase.getIngredientData(mashBucket.getFlavorIngredient().getType());

		// Create Base item
		ItemStack finalBrew = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) finalBrew.getItemMeta();

		// Apply Potion Effect
		if(mashBucket.getSecondaryIngredient() != null)
		{
			// Get Base score
			double rawScore = secondaryIngredientData.getRarity().getEffectPotency();
			// Scale score based on number of ingredients, i.e. do we have enough material
			if(mashBucket.getPrimaryIngredient().getAmount() > mashBucket.getSecondaryIngredient().getAmount())
			{
				rawScore = rawScore * (mashBucket.getSecondaryIngredient().getAmount()/mashBucket.getPrimaryIngredient().getAmount());
			}

			// Scale score based on how much we have aged, i.e. did we age long enough
			rawScore *= Math.min(age / type.getAgingRequirement(), 1.0);
			
			// Get Effect type, level, and duration
			PotionEffectType effect = secondaryIngredientData.getPotionEffectType();
			int effectLevel = Math.min(type.getLevelCap(), (int)(rawScore/100 * (type.getLevelCap()))); // In level
			int durationScore = effect.isInstant() ? 0 : Math.min(type.getDurationCap(), (int)(type.getDurationCap() * (rawScore / 100))); // In ticks

			potionMeta.addCustomEffect(new PotionEffect(
					effect, // Effect Type 
					durationScore, // Duration 
					effectLevel, // Level
					false, false, false), true);
		}

		// Get Recipe
		DrinkRecipe drinkRecipe = mashBucket.getDrinkRecipe(type.toString());

		BreweryRecipe recipe = null;
		try {
			recipe = DatabaseCommunication.getRecipe(
					Bukkit.getPlayer(BreweryUtils.getUUID(mashBucket.getCrafter())),
					drinkRecipe.hashCode());
		} catch (ParseException | org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}

		// Set Meta
		// Setup flavortext
		ArrayList<String> fullFlavorText = new ArrayList<String>();

		// Drink Type
		String drinkType = drinkRecipe.getAlcoholLevel() > 0 ? primaryIngredientData.getAlcoholicDrinkName() : primaryIngredientData.getDrinkName();

		// Flavor
		if(drinkRecipe.getFlavorIngredient() != null)
		{
			drinkType += ", " + flavorIngredientData.getFlavorDescriptor() + " flavor";
		}

		String secondLine = "";
		// Alcohol
		if(drinkRecipe.getAlcoholLevel() > 0)
		{
			secondLine += drinkRecipe.getAlcoholLevel() + " Proof";
		}

		// Age
		secondLine += getAgedString(age, type, drinkRecipe.getAlcoholLevel() > 0);

		// Construct flavor text list
		fullFlavorText.add(drinkType);
		if(!secondLine.isEmpty())
		{
			fullFlavorText.add(secondLine);
		}
		fullFlavorText.addAll(recipe.getFlavorText());

		potionMeta.setLore(fullFlavorText);
		potionMeta.setDisplayName(recipe.getName());
		potionMeta.setColor(primaryIngredientData.getColor());
		finalBrew.setItemMeta(potionMeta);

		// Set NBT tags
		NBTItem nbti = new NBTItem(finalBrew);
		NBTCompound newBreweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING);
		newBreweryMeta.setInteger(NBTConstants.ALCOHOL_LEVEL_TAG_STRING, drinkRecipe.getAlcoholLevel());
		newBreweryMeta.setString(NBTConstants.CRAFTER_TAG_STRING, mashBucket.getCrafter());

		finalBrew = nbti.getItem();

		return finalBrew;
	}

	private static ItemStack getRuinedBrew() {		
		ItemStack item = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

		potionMeta.setDisplayName("Ruined Brew");

		ArrayList<String> agedFlavorText = new ArrayList<String>();
		agedFlavorText.add("A botched brew.");
		potionMeta.setLore(agedFlavorText);
		potionMeta.clearCustomEffects();

		item.setItemMeta(potionMeta);

		return item;
	}

	private static String getAgedString(int age, BarrelType type, boolean isAlcoholic)
	{
		if(type == BarrelType.OAK)
		{
			return "";
		}
		else
		{
			String rValue = "";
			if(!isAlcoholic)
			{
				rValue += "A";
			}
			else
			{
				rValue += ", a";
			}
			
			// Handle Plural
			if(age == 1)
			{
				rValue+= "ged 1 year";
			}
			else
			{
				rValue+="ged " + age + " years"; 
			}
			return rValue;
		}
	}
}
