package com.dreamless.brewery.brew;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.data.DatabaseCommunication;
import com.dreamless.brewery.data.NBTConstants;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BrewItemFactory {
	
	// Fermentation
	private static final double MAXIMUM_VALUE_SCALE = 1.0;
	private static final int FALLOFF_START = 10;
	private static final double MINIMUM_VALUE_SCALE = 0.25;
	private static final double FALLOFF_RATE = 0.3;
	private static final int SATURATION_LIMIT = 100;

	private static enum BrewState{
		FERMENTED, DISTILLED, FINISHED, RUINED;
	}

	public static ItemStack getFermentedBrew(Player player, Inventory cauldronInventory, int cookTime) {
		int optimalCookTime = 0;

		// Add together the value of the ingredients
		HashMap<AspectRarityPair, Double> aspectRaritySaturationMap = new HashMap< AspectRarityPair, Double>();
		HashMap<Aspect, Integer> aspectItemCountMap = new HashMap< Aspect, Integer>();
		for(ItemStack itemStack : cauldronInventory.getContents())
		{
			if(itemStack == null) {
				continue;
			}
			ArrayList<Aspect> aspects = Aspect.getAspect(itemStack.getType());
			Rarity rarity = Rarity.getRarity(itemStack.getType());
			for(Aspect aspect : aspects)
			{
				AspectRarityPair pairedAspectRarity = new  AspectRarityPair(aspect, rarity);
				aspectRaritySaturationMap.put(pairedAspectRarity, aspectRaritySaturationMap.getOrDefault(pairedAspectRarity, 0.0) + rarity.getSaturationBonus() * itemStack.getAmount());
				aspectItemCountMap.put(aspect, aspectItemCountMap.getOrDefault(aspect, 0) + (itemStack.getAmount()*rarity.getItemContribution()));
			}
			optimalCookTime += rarity.getCookTime() * itemStack.getAmount();
		}
		
		//Brewery.breweryDriver.debugLog("AICM: " + aspectItemCountMap.toString());
		//Brewery.breweryDriver.debugLog("ARSM: " + aspectRaritySaturationMap.toString());

		// Calculate Saturation Scores
		HashMap<Aspect, Double> aspectSaturationScoreMap = new HashMap<Aspect, Double>();
		for(Entry< AspectRarityPair, Double> entry : aspectRaritySaturationMap.entrySet()) {
			aspectSaturationScoreMap.put(
					entry.getKey().aspect, // Aspect 
					aspectSaturationScoreMap.getOrDefault(entry.getKey().aspect, 0.0) + // The current value
					Math.min(entry.getKey().rarity.getSaturationCap(), entry.getValue())); // The new value to add
		}
		
		//Brewery.breweryDriver.debugLog("ASSM: " + aspectSaturationScoreMap.toString());
		
		// Calculate final Aspect Score
		HashMap<Aspect, Integer> aspectMap = new HashMap<Aspect, Integer>();		
		for(Entry< Aspect, Double> entry : aspectSaturationScoreMap.entrySet()) {
			aspectMap.put(
					entry.getKey(),  
					(int) Math.ceil(Math.min(aspectItemCountMap.get(entry.getKey()), SATURATION_LIMIT) * // Item Score
					(1 + entry.getValue()))); //  Saturation Score
		}
		
		//Brewery.breweryDriver.debugLog("AM1: " + aspectMap.toString());

		// Calculate Cook time scaling
		int deviation = Math.abs(optimalCookTime - cookTime);
		//Brewery.breweryDriver.debugLog("OPT: " + optimalCookTime + " ACT: " + cookTime);
		final double scalar = (deviation < FALLOFF_START) ? 
				MAXIMUM_VALUE_SCALE :  Math.max(MAXIMUM_VALUE_SCALE - (deviation * FALLOFF_RATE), MINIMUM_VALUE_SCALE);

		// Update Map
		aspectMap.forEach((aspect, value) -> aspectMap.replace(aspect, (int)(value * scalar)));  
		
		//Brewery.breweryDriver.debugLog("AM2: " + aspectMap.toString());


		// Create Item
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// Set Name
		potionMeta.setDisplayName(Brewery.getText("Brew_UnfinishedPotion"));
		potionMeta.setColor(Color.ORANGE);
		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING); // All brewery NBT gets set here.
		NBTCompound aspects = breweryMeta.addCompound(NBTConstants.ASPECTS_TAG_STRING);

		for(Entry<Aspect, Integer> entry : aspectMap.entrySet()) {
			aspects.setInteger(entry.getKey().toString(), entry.getValue());
		}

		// Crafter
		breweryMeta.setString(NBTConstants.CRAFTER_TAG_STRING, player.getName());

		// Set state
		breweryMeta.setString(NBTConstants.STATE_TAG_STRING, BrewState.FERMENTED.toString());

		// Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}

	public static void getDistilledBrews(BrewerInventory brewerInventory, Inventory filterInventory) {

		AspectMatrix matrix = new AspectMatrix();

		for(ItemStack item : filterInventory) {
			if(item != null) {
				matrix.distillAspect(Aspect.getFilterAspect(item.getType()), item.getAmount());
			}
		}

		// Get all potential effects
		BreweryEffect effect = BreweryEffect.getEffect(matrix);

		for(int i = 0; i < 3; i++) {
			ItemStack item = brewerInventory.getItem(i);
			if(item == null || item.getType() != Material.POTION) {
				continue;
			}

			//Set NBT
			NBTItem nbti = new NBTItem(item);
			if(!nbti.hasKey(NBTConstants.BREWERY_TAG_STRING))
			{
				continue;
			}

			brewerInventory.setItem(i, getDistilledBrew(nbti, effect));	
		}		
	}

	private static ItemStack getDistilledBrew(NBTItem item, BreweryEffect effect) {
		
		// Extract info
		NBTCompound originalNBT = item.getCompound(NBTConstants.BREWERY_TAG_STRING);
		NBTCompound itemAspects = originalNBT.getCompound(NBTConstants.ASPECTS_TAG_STRING);
		if(itemAspects == null || effect == BreweryEffect.NONE) {
			return getRuinedBrew();
		}
		
		try {
			if(BrewState.valueOf(originalNBT.getString(NBTConstants.STATE_TAG_STRING)) != BrewState.FERMENTED) {
				return getRuinedBrew();
			}
		} catch (Exception e) {
			return getRuinedBrew();
		}
		
		
		HashMap<Aspect, Integer> aspectContents = new HashMap<Aspect, Integer>();
		for(String aspects : itemAspects.getKeys()) {
			aspectContents.put(Aspect.valueOf(aspects), itemAspects.getInteger(aspects));
		}
		
		// Create Item
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// Set Name
		potionMeta.setDisplayName(Brewery.getText("Brew_DistilledPotion"));
		potionMeta.setColor(Color.WHITE);
		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING); // All brewery NBT gets set here.

		// Set state
		breweryMeta.setString(NBTConstants.CRAFTER_TAG_STRING, originalNBT.getString(NBTConstants.CRAFTER_TAG_STRING));
		breweryMeta.setString(NBTConstants.STATE_TAG_STRING, BrewState.DISTILLED.toString());
		
		// Write Effects
		breweryMeta.setString(NBTConstants.EFFECT_NAME_TAG_STRING, effect.name());
		breweryMeta.setInteger(NBTConstants.EFFECT_SCORE_TAG_STRING, effect.getEffectStrength(aspectContents));

		// Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}

	
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

		//Set NBT
		NBTItem nbti = new NBTItem(item);

		//Tag as distilling brew
		NBTCompound breweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING);
		breweryMeta.setInteger(NBTConstants.STATE_TAG_STRING, BrewState.RUINED.ordinal());

		return item;
	}
	
	public static String extractEffectKey(ItemStack itemStack) {
		
		NBTItem item = new NBTItem(itemStack);
		
		NBTCompound breweryMeta = item.getCompound(NBTConstants.BREWERY_TAG_STRING);
		
		if(breweryMeta == null) {
			return null;
		}
		
		String effectkey = "";
		effectkey = effectkey.concat(breweryMeta.getString(NBTConstants.EFFECT_NAME_TAG_STRING));
		effectkey = effectkey.concat("-");
		effectkey = effectkey.concat(breweryMeta.getInteger(NBTConstants.POTENCY_SCORE_TAG_STRING).toString());
		effectkey = effectkey.concat("-");
		effectkey = effectkey.concat(breweryMeta.getInteger(NBTConstants.DURATION_SCORE_TAG_STRING).toString());
		
		return effectkey;
	}
	
}
