package com.dreamless.brewery.brew;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.dreamless.brewery.data.NBTConstants;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class MashBucket {

	private final ItemStack primaryIngredient;
	private final ItemStack secondaryIngredient;
	private final ItemStack flavorIngredient;
	private final ItemStack alcoholIngredient;

	public MashBucket(ItemStack primary, ItemStack secondary, ItemStack flavor, ItemStack alcohol)
	{
		primaryIngredient = primary;
		secondaryIngredient = secondary;
		flavorIngredient = flavor;
		alcoholIngredient = alcohol;
	}

	private ArrayList<String> getFlavorText()
	{
		// Primary
		String flavourText = "A bucket of mashed " + primaryIngredient.getType().toString();

		// Secondary
		if(secondaryIngredient != null)
		{
			flavourText += " mixed with " + secondaryIngredient.getType().toString();
		}

		// Flavor
		if(flavorIngredient != null)
		{
			flavourText += " and flavoured with " + flavorIngredient.getType().toString();
		}

		flavourText += "."; // End this.

		// Alcohol
		if(alcoholIngredient != null)
		{

			flavourText += " This mash is ";
			switch(alcoholIngredient.getAmount())
			{
			case 1:
				flavourText += " slightly ";
				break;
			case 2:
			case 3:
				flavourText += " moderately ";
				break;
			case 4:
			case 5:
				flavourText += " moderately ";
			}
			flavourText += " alcoholic.";
		}

		// Word Wrap
		ArrayList<String> flavourList = new ArrayList<String>();
		flavourList.addAll(BreweryUtils.wordWrap(flavourText));

		// Return
		return flavourList;
	}

	private String getName()
	{
		String name = "Bucket of Mashed " + primaryIngredient.getType().toString();
		return name;
	}

	public ItemStack getItem()
	{
		ItemStack mashBucket = new ItemStack(Material.WATER_BUCKET);

		////////////////////////////////////
		// Set NBT Data
		////////////////////////////////////
		NBTItem nbti = new NBTItem(mashBucket);

		NBTCompound nbtCompound = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING);
		nbtCompound.setString(NBTConstants.ITEM_TYPE_STRING, NBTConstants.MASH_BUCKET_TAG_STRING);

		// Primary NBT
		nbtCompound.setString(NBTConstants.MASH_BUCKET_PRIMARY_STRING, primaryIngredient.getType().name());
		nbtCompound.setInteger(NBTConstants.MASH_BUCKET_PRIMARY_COUNT, primaryIngredient.getAmount());

		// Secondary NBT
		if(secondaryIngredient != null)
		{
			nbtCompound.setString(NBTConstants.MASH_BUCKET_PRIMARY_STRING, secondaryIngredient.getType().name());
			nbtCompound.setInteger(NBTConstants.MASH_BUCKET_PRIMARY_COUNT, secondaryIngredient.getAmount());
		}

		// Flavor NBT
		if(flavorIngredient != null)
		{
			nbtCompound.setString(NBTConstants.MASH_BUCKET_PRIMARY_STRING, flavorIngredient.getType().name());
			nbtCompound.setInteger(NBTConstants.MASH_BUCKET_PRIMARY_COUNT, flavorIngredient.getAmount());
		}

		// Alcohol
		if(alcoholIngredient != null)
		{
			nbtCompound.setString(NBTConstants.MASH_BUCKET_ALCOHOL_STRING, alcoholIngredient.getType().name());
			nbtCompound.setInteger(NBTConstants.MASH_BUCKET_ALCOHOL_COUNT, alcoholIngredient.getAmount());
		}

		mashBucket = nbti.getItem();

		////////////////////////////////////
		// Set Flavour text
		////////////////////////////////////
		ItemMeta mashBucketMeta = mashBucket.getItemMeta();
		mashBucketMeta.setDisplayName(getName());
		mashBucketMeta.setLore(getFlavorText());
		mashBucket.setItemMeta(mashBucketMeta);

		////////////////////////////////////
		// Finish
		////////////////////////////////////

		return new ItemStack(Material.WATER_BUCKET);
	}

	public ArrayList<ItemStack> getContents()
	{
		ArrayList<ItemStack> contents = new ArrayList<ItemStack>();
		contents.add(primaryIngredient);
		if(secondaryIngredient != null)
		{
			contents.add(secondaryIngredient);
		}

		if(flavorIngredient != null)
		{
			contents.add(flavorIngredient);
		}

		if(alcoholIngredient != null)
		{
			contents.add(alcoholIngredient);
		}
		return contents;
	}
}
