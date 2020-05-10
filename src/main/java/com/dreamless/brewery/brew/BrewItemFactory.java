package com.dreamless.brewery.brew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.dreamless.brewery.Brewery;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BrewItemFactory {

	private static final double MAXIMUM_VALUE_SCALE = 1.0;
	private static final int FALLOFF_START = 10;
	private static final double MINIMUM_VALUE_SCALE = 0.25;
	private static final double FALLOFF_RATE = 0.3;

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
		NBTCompound breweryMeta = nbti.addCompound("brewery"); // All brewery NBT gets set here.
		NBTCompound aspects = breweryMeta.addCompound("aspects");

		for(Entry<Aspect, Integer> entry : aspectMap.entrySet()) {
			aspects.setInteger(entry.getKey().toString(), entry.getValue());
		}

		// Crafter
		NBTCompound crafters = breweryMeta.addCompound("crafters");
		crafters.setString(player.getDisplayName(), player.getDisplayName());

		// Set state
		breweryMeta.setInteger("state", BrewState.FERMENTED.ordinal());

		// Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}

	public static void doDistillBrews(BrewerInventory brewerInventory, Inventory filterInventory) {

		AspectMatrix matrix = new AspectMatrix();

		for(ItemStack item : filterInventory) {
			if(item != null) {
				matrix.distillAspect(Aspect.getFilterAspect(item.getType()), item.getAmount());
			}
		}

		HashSet<BreweryEffect> set = BreweryEffect.getEffectsMatrix(matrix);

		for(int i = 0; i < 3; i++) {
			ItemStack item = brewerInventory.getItem(i);
			if(item == null || item.getType() != Material.POTION) {
				continue;
			}

			//Set NBT
			NBTItem nbti = new NBTItem(item);
			if(!nbti.hasKey("brewery"))
			{
				continue;
			}

			brewerInventory.setItem(i, getDistilledPotion(nbti, set, matrix));	
		}		
	}

	private static ItemStack getDistilledPotion(NBTItem item, HashSet<BreweryEffect> set, AspectMatrix matrix) {
		
		// Extract info
		NBTCompound itemAspects = item.getCompound("brewery").getCompound("aspects");
		if(itemAspects == null) {
			return null;
		}
		
		HashMap<Aspect, Integer> aspectContents = new HashMap<Aspect, Integer>();
		for(String aspects : itemAspects.getKeys()) {
			aspectContents.put(Aspect.valueOf(aspects), itemAspects.getInteger(aspects));
		}
		

		HashMap<BreweryEffect, Integer> set2 = BreweryEffect.getEffects(matrix, aspectContents);
		
		// Create Item
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// Set Name
		potionMeta.setDisplayName(Brewery.getText("Brew_UnfinishedPotion")); // TODO: Brew_DistilledPotion
		potionMeta.setColor(Color.ORANGE); //TODO: Placeholder colour, make it drink-driven
		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound("brewery"); // All brewery NBT gets set here.

		// Set state
		breweryMeta.setInteger("state", BrewState.DISTILLED.ordinal());
		
		// Write Effects
		for(Entry<BreweryEffect, Integer> entry : set2.entrySet()) {
			breweryMeta.setInteger(entry.getKey().toString(), entry.getValue());
		}

		// Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}

	public static ItemStack getRuinedPotion() {		
		ItemStack item = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

		potionMeta.setDisplayName("Ruined Brew");

		ArrayList<String> agedFlavorText = new ArrayList<String>();
		agedFlavorText.add("A brew that was ruined");
		potionMeta.setLore(agedFlavorText);

		potionMeta.clearCustomEffects();

		item.setItemMeta(potionMeta);

		//Set NBT
		NBTItem nbti = new NBTItem(item);

		//Tag as distilling brew
		NBTCompound breweryMeta = nbti.getCompound("brewery");
		breweryMeta.setInteger("state", BrewState.RUINED.ordinal());

		return item;
	}

}
