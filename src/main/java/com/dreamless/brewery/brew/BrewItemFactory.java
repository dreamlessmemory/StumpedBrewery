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
import com.dreamless.brewery.data.RecipeEntry;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BrewItemFactory {
	
	
	// NBT Keys
	

	// Fermentation
	private static final double MAXIMUM_VALUE_SCALE = 1.0;
	private static final int FALLOFF_START = 10;
	private static final double MINIMUM_VALUE_SCALE = 0.25;
	private static final double FALLOFF_RATE = 0.3;
	
	// Aging

	private static enum BrewState{
		FERMENTED, DISTILLED, FINISHED, RUINED;
	}

	public static ItemStack getFermentedBrew(Player player, Inventory cauldronInventory, int cookTime) {
		int optimalCookTime = 0;

		HashMap<AspectRarityPair, Integer> tempMap = new HashMap< AspectRarityPair, Integer>();
		for(ItemStack itemStack : cauldronInventory.getContents())
		{
			if(itemStack == null) {
				continue;
			}
			Aspect aspect = Aspect.getAspect(itemStack.getType());
			Rarity rarity = Rarity.getRarity(itemStack.getType());
			AspectRarityPair pairedAspectRarity = new  AspectRarityPair(aspect, rarity);
			tempMap.put(pairedAspectRarity, tempMap.getOrDefault(pairedAspectRarity, 0) + rarity.getValue());
			optimalCookTime += rarity.getCookTime();
		}

		// Set up final map
		HashMap<Aspect, Integer> aspectMap = new HashMap<Aspect, Integer>();
		for(Entry< AspectRarityPair, Integer> entry : tempMap.entrySet()) {
			aspectMap.put(entry.getKey().aspect, aspectMap.getOrDefault(entry.getKey().aspect, 0) +
					Math.max(entry.getKey().rarity.getSaturation(), entry.getValue()));
		}

		int deviation = Math.abs(optimalCookTime - cookTime);
		final double scalar = (deviation < FALLOFF_START) ? 
				MAXIMUM_VALUE_SCALE :  Math.max(MAXIMUM_VALUE_SCALE - (deviation * FALLOFF_RATE), MINIMUM_VALUE_SCALE);

		// Update Map
		aspectMap.forEach((aspect, value) -> aspectMap.replace(aspect, (int)(value * scalar)));  		

		// Create Item
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// Set Name
		potionMeta.setDisplayName(Brewery.getText("Brew_UnfinishedPotion"));
		potionMeta.setColor(Color.ORANGE); //TODO: Placeholder colour, make it drink-driven
		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING); // All brewery NBT gets set here.
		NBTCompound aspects = breweryMeta.addCompound(NBTConstants.ASPECTS_TAG_STRING);

		for(Entry<Aspect, Integer> entry : aspectMap.entrySet()) {
			aspects.setInteger(entry.getKey().toString(), entry.getValue());
		}

		// Crafter
		breweryMeta.setString(NBTConstants.CRAFTER_TAG_STRING, player.getDisplayName());

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
		potionMeta.setDisplayName(Brewery.getText("Brew_UnfinishedPotion")); // TODO: Brew_DistilledPotion
		potionMeta.setColor(effect.getColor());
		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING); // All brewery NBT gets set here.

		// Set state
		breweryMeta.setString(NBTConstants.CRAFTER_TAG_STRING, originalNBT.getString(NBTConstants.CRAFTER_TAG_STRING));
		breweryMeta.setString(NBTConstants.STATE_TAG_STRING, BrewState.DISTILLED.toString());
		
		// Write Effects
		//for(Entry<BreweryEffect, Integer> entry : set2.entrySet()) {
		breweryMeta.setString(NBTConstants.EFFECT_NAME_TAG_STRING, effect.name());
		breweryMeta.setInteger(NBTConstants.EFFECT_SCORE_TAG_STRING, effect.getEffectStrength(aspectContents));
		//}

		// Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}

	// TODO: Implement
	public static ItemStack getAgedBrew(ItemStack item, int age, BarrelType type) {
		
		NBTItem nbti = new NBTItem(item);
		NBTCompound breweryMeta = nbti.getCompound(NBTConstants.BREWERY_TAG_STRING);
		
		if(breweryMeta == null)
		{
			return item;
		}
		
		try {
			if(BrewState.valueOf(breweryMeta.getString(NBTConstants.STATE_TAG_STRING)) != BrewState.DISTILLED) {
				return getRuinedBrew();
			}
		} catch (Exception e) {
			return getRuinedBrew();
		}
		
		//TODO: Calculate effect score
		BreweryEffect effect;
		try {
			effect = BreweryEffect.valueOf(breweryMeta.getString(NBTConstants.EFFECT_NAME_TAG_STRING));
		} catch (Exception e) {
			return getRuinedBrew();
		}
		int potencyScore = breweryMeta.getInteger(NBTConstants.EFFECT_SCORE_TAG_STRING) * type.getLevelIncrease();
		int durationScore = breweryMeta.getInteger(NBTConstants.EFFECT_SCORE_TAG_STRING) * type.getDurationIncrease();
		
		
		//TODO: Apply potion effect
		ItemStack finalBrew = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) finalBrew.getItemMeta();
		potionMeta.addCustomEffect(new PotionEffect(effect.getPotionEffectType(), 
				effect.getEffectDuration(potencyScore, durationScore), 
				effect.getEffectLevel(potencyScore, durationScore), 
				false, false, false), true);
		
		RecipeEntry entry = new RecipeEntry(effect, potencyScore, durationScore);
		
		//TODO: Lookup recipe
		BreweryRecipe recipe;
		try {
			recipe = DatabaseCommunication.getRecipe(
					Bukkit.getPlayer(BreweryUtils.getUUID(breweryMeta.getString(NBTConstants.CRAFTER_TAG_STRING))),
					entry);
		} catch (ParseException | org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return getRuinedBrew();
		}
		
		potionMeta.setLore(recipe.getFlavorText());
		potionMeta.setDisplayName(recipe.getName());
		finalBrew.setItemMeta(potionMeta);
		
		// Set NBT tags
		nbti = new NBTItem(finalBrew);
		NBTCompound newBreweryMeta = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING);
		newBreweryMeta.setString(NBTConstants.EFFECT_NAME_TAG_STRING, effect.toString());
		newBreweryMeta.setInteger(NBTConstants.POTENCY_SCORE_TAG_STRING, entry.getPotencyScore());
		newBreweryMeta.setInteger(NBTConstants.DURATION_SCORE_TAG_STRING, entry.getDurationScore());
		newBreweryMeta.setString(NBTConstants.CRAFTER_TAG_STRING, breweryMeta.getString(NBTConstants.CRAFTER_TAG_STRING));
		newBreweryMeta.setString(NBTConstants.STATE_TAG_STRING, BrewState.FINISHED.toString());
		
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
		NBTCompound breweryMeta = nbti.getCompound(NBTConstants.BREWERY_TAG_STRING);
		breweryMeta.setInteger(NBTConstants.STATE_TAG_STRING, BrewState.RUINED.ordinal());

		return item;
	}	
}
