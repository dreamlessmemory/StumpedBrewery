package com.dreamless.brewery.brew;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
		FERMENTED, DISTILLED, FINISHED;
	}

	public static ItemStack getFermentedBrew(Player player, Inventory inventory, int cookTime) {
		
		int optimalCookTime = 0;
		
		HashMap<AspectRarityPair, Integer> tempMap = new HashMap< AspectRarityPair, Integer>();
		for(ItemStack itemStack : inventory.getContents())
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
		
		
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// Set Name
		potionMeta.setDisplayName(Brewery.getText("Brew_UnfinishedPotion"));
		potionMeta.setColor(Color.ORANGE); //TODO: Placeholder colour, make it drink-driven
		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound("brewery"); // All brewery NBT gets set here.

		for(Entry<Aspect, Integer> entry : aspectMap.entrySet()) {
			breweryMeta.setInteger(entry.getKey().toString(), entry.getValue());
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
	
}
