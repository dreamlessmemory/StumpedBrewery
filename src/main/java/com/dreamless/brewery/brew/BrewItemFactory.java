package com.dreamless.brewery.brew;

import java.text.ParseException;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import com.dreamless.brewery.data.DatabaseCommunication;
import com.dreamless.brewery.data.NBTConstants;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BrewItemFactory {

	public static ItemStack getAgedBrew(MashBucket item, int age, BarrelType type) 
	{
		if(item == null)
		{
			return getRuinedBrew();
		}


		IngredientData primaryIngredientData = IngredientDatabase.getIngredientData(item.getPrimaryIngredient().getType());
		IngredientData secondaryIngredientData = IngredientDatabase.getIngredientData(item.getSecondaryIngredient().getType());
		IngredientData flavorIngredientData = IngredientDatabase.getIngredientData(item.getFlavorIngredient().getType());

		int potencyScore = Math.min((int)Math.ceil(secondaryIngredientData.getRarity().getEffectPotency()/type.getAgingFactor()), age) * type.getLevelIncrease();
		int durationScore = Math.min((int)Math.ceil(secondaryIngredientData.getRarity().getEffectPotency()/type.getAgingFactor()), age) * type.getDurationIncrease();

		// Create Base item
		ItemStack finalBrew = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) finalBrew.getItemMeta();
		potionMeta.addCustomEffect(new PotionEffect(secondaryIngredientData.getPotionEffectType(), 
				BreweryEffect.calcuateEffectDuration(secondaryIngredientData.getPotionEffectType(), durationScore, type), 
				BreweryEffect.calculateEffectLevel(secondaryIngredientData.getPotionEffectType(), potencyScore, durationScore, type), 
				false, false, false), true);

		// Get Recipe
		DrinkRecipe drinkRecipe = item.getDrinkRecipe(type.toString());

		BreweryRecipe recipe = null;
		try {
			recipe = DatabaseCommunication.getRecipe(
					Bukkit.getPlayer(BreweryUtils.getUUID(item.getCrafter())),
					drinkRecipe.hashCode());
		} catch (ParseException | org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}

		// Set Meta
		// Setup flavortext
		ArrayList<String> fullFlavorText = new ArrayList<String>();

		// Drink Type
		String drinkType = primaryIngredientData.getDrinkName();

		// Flavor
		if(drinkRecipe.getFlavorIngredient() != null)
		{
			drinkType += ", " + flavorIngredientData.getFlavorDescriptor() + " flavor";
		}

		// Alcohol
		if(drinkRecipe.getAlcoholLevel() > 0)
		{
			drinkType += ", " + drinkRecipe.getAlcoholLevel() + " Proof";
		}
		// Construct flavor text list
		fullFlavorText.add(drinkType);
		fullFlavorText.addAll(recipe.getFlavorText());

		potionMeta.setLore(fullFlavorText);
		potionMeta.setDisplayName(recipe.getName());
		potionMeta.setColor(primaryIngredientData.getColor());
		finalBrew.setItemMeta(potionMeta);

		// Set NBT tags
		NBTItem nbti = new NBTItem(finalBrew);
		NBTCompound newBreweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING);
		newBreweryMeta.setString(NBTConstants.EFFECT_NAME_TAG_STRING, secondaryIngredientData.getPotionEffectType().toString());
		newBreweryMeta.setInteger(NBTConstants.POTENCY_SCORE_TAG_STRING, potencyScore);
		newBreweryMeta.setInteger(NBTConstants.DURATION_SCORE_TAG_STRING, durationScore);
		newBreweryMeta.setInteger(NBTConstants.EFFECT_SCORE_TAG_STRING, secondaryIngredientData.getRarity().getEffectPotency());
		newBreweryMeta.setInteger(NBTConstants.ALCOHOL_LEVEL_TAG_STRING, drinkRecipe.getAlcoholLevel());
		newBreweryMeta.setString(NBTConstants.CRAFTER_TAG_STRING, item.getCrafter());

		finalBrew = nbti.getItem();

		return finalBrew;
	}

	public static ItemStack getRuinedBrew() {		
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

}
