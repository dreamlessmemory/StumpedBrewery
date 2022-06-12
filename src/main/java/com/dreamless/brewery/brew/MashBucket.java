package com.dreamless.brewery.brew;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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

	//////////////////////////////////
	/// <pre> bucket is a MashBucket
	//////////////////////////////////
	public MashBucket(ItemStack bucket)
	{
		NBTItem nbti = new NBTItem(bucket);
		NBTCompound nbtCompound= nbti.getCompound(NBTConstants.BREWERY_TAG_STRING);

		//Primary
		primaryIngredient =
				new ItemStack(
						Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_PRIMARY_STRING)), 
						nbtCompound.getInteger(NBTConstants.BUCKET_PRIMARY_COUNT));

		//Secondary
		secondaryIngredient = 
				(nbtCompound.hasKey(NBTConstants.BUCKET_SECONDARY_STRING) && 
						nbtCompound.hasKey(NBTConstants.BUCKET_SECONDARY_COUNT)) ?
								new ItemStack(
										Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_SECONDARY_STRING)), 
										nbtCompound.getInteger(NBTConstants.BUCKET_SECONDARY_COUNT)) :
											null;		
		//Flavour
		flavorIngredient = 
				(nbtCompound.hasKey(NBTConstants.BUCKET_FLAVOUR_STRING) && 
						nbtCompound.hasKey(NBTConstants.BUCKET_FLAVOUR_COUNT)) ?
								new ItemStack(
										Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_FLAVOUR_STRING)), 
										nbtCompound.getInteger(NBTConstants.BUCKET_FLAVOUR_COUNT)) :
											null;	

		//Alcohol
		alcoholIngredient = 
				(nbtCompound.hasKey(NBTConstants.BUCKET_ALCOHOL_STRING) && 
						nbtCompound.hasKey(NBTConstants.BUCKET_ALCOHOL_COUNT)) ?
								new ItemStack(
										Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_ALCOHOL_STRING)), 
										nbtCompound.getInteger(NBTConstants.BUCKET_ALCOHOL_COUNT)) :
											null;	
	}

	public static boolean isMashBucket(ItemStack item)
	{
		NBTItem nbti = new NBTItem(item);
		NBTCompound nbtCompound= nbti.getCompound(NBTConstants.BREWERY_TAG_STRING);
		if(nbtCompound != null && 
				nbtCompound.getString(NBTConstants.ITEM_TYPE_STRING).equals(NBTConstants.MASH_BUCKET_TAG_STRING))
		{
			return true;
		}

		return false;
	}
	
	public static boolean isFermentedBucket(ItemStack item)
	{
		NBTItem nbti = new NBTItem(item);
		NBTCompound nbtCompound= nbti.getCompound(NBTConstants.BREWERY_TAG_STRING);
		if(nbtCompound != null && 
				nbtCompound.getString(NBTConstants.ITEM_TYPE_STRING).equals(NBTConstants.FERMENTED_BUCKET_TAG_STRING))
		{
			return true;
		}

		return false;
	}

	public static void dumpContents(ItemStack bucket, Location location)
	{
		if(!isMashBucket(bucket))
		{
			return;
		}

		ArrayList<ItemStack> contents = new ArrayList<ItemStack>();
		NBTItem nbti = new NBTItem(bucket);
		NBTCompound nbtCompound= nbti.getCompound(NBTConstants.BREWERY_TAG_STRING);

		//Primary
		if(nbtCompound.hasKey(NBTConstants.BUCKET_PRIMARY_STRING) && nbtCompound.hasKey(NBTConstants.BUCKET_PRIMARY_COUNT))
		{
			contents.add(
					new ItemStack(
							Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_PRIMARY_STRING)), 
							nbtCompound.getInteger(NBTConstants.BUCKET_PRIMARY_COUNT)));
		}
		else 
		{
			return; // malformed
		}

		//Secondary
		if(nbtCompound.hasKey(NBTConstants.BUCKET_SECONDARY_STRING) && nbtCompound.hasKey(NBTConstants.BUCKET_SECONDARY_COUNT))
		{
			contents.add(
					new ItemStack(
							Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_SECONDARY_STRING)), 
							nbtCompound.getInteger(NBTConstants.BUCKET_SECONDARY_COUNT)));
		}

		//Flavour
		if(nbtCompound.hasKey(NBTConstants.BUCKET_FLAVOUR_STRING) && nbtCompound.hasKey(NBTConstants.BUCKET_FLAVOUR_COUNT))
		{
			contents.add(
					new ItemStack(
							Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_FLAVOUR_STRING)), 
							nbtCompound.getInteger(NBTConstants.BUCKET_FLAVOUR_COUNT)));
		}

		//Alcohol
		if(nbtCompound.hasKey(NBTConstants.BUCKET_ALCOHOL_STRING) && nbtCompound.hasKey(NBTConstants.BUCKET_ALCOHOL_COUNT))
		{
			contents.add(
					new ItemStack(
							Material.getMaterial(nbtCompound.getString(NBTConstants.BUCKET_ALCOHOL_STRING)), 
							nbtCompound.getInteger(NBTConstants.BUCKET_ALCOHOL_COUNT)));
		}

		// Drop everything relevant
		for (ItemStack item : contents) {
			if (item != null) {
				location.getWorld().dropItemNaturally(location, item);
				location.getWorld().playSound(location, Sound.ENTITY_ITEM_PICKUP,
						(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
			}
		}
	}

	public ItemStack getItem()
	{
		ItemStack mashBucket = getNBTBucket(NBTConstants.MASH_BUCKET_TAG_STRING);

		// Set Flavour text
		ItemMeta mashBucketMeta = mashBucket.getItemMeta();
		mashBucketMeta.setDisplayName(getName());
		mashBucketMeta.setLore(getFlavorText());
		mashBucket.setItemMeta(mashBucketMeta);

		return mashBucket;
	}

	public ItemStack getFermentedItem()
	{
		ItemStack mashBucket = getNBTBucket(NBTConstants.FERMENTED_BUCKET_TAG_STRING);

		// Set Flavour text
		ItemMeta mashBucketMeta = mashBucket.getItemMeta();
		mashBucketMeta.setDisplayName(getFermentedName());
		mashBucketMeta.setLore(getFermentedFlavorText());
		mashBucket.setItemMeta(mashBucketMeta);
		return mashBucket;
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

	private ItemStack getNBTBucket(String bucketTypeString)
	{
		ItemStack mashBucket = new ItemStack(Material.WATER_BUCKET);

		////////////////////////////////////
		// Set NBT Data
		////////////////////////////////////
		NBTItem nbti = new NBTItem(mashBucket);

		NBTCompound nbtCompound = nbti.addCompound(NBTConstants.BREWERY_TAG_STRING);
		nbtCompound.setString(NBTConstants.ITEM_TYPE_STRING, bucketTypeString);

		// Primary NBT
		nbtCompound.setString(NBTConstants.BUCKET_PRIMARY_STRING, primaryIngredient.getType().name());
		nbtCompound.setInteger(NBTConstants.BUCKET_PRIMARY_COUNT, primaryIngredient.getAmount());

		// Secondary NBT
		if(secondaryIngredient != null)
		{
			nbtCompound.setString(NBTConstants.BUCKET_SECONDARY_STRING, secondaryIngredient.getType().name());
			nbtCompound.setInteger(NBTConstants.BUCKET_SECONDARY_COUNT, secondaryIngredient.getAmount());
		}

		// Flavor NBT
		if(flavorIngredient != null)
		{
			nbtCompound.setString(NBTConstants.BUCKET_FLAVOUR_STRING, flavorIngredient.getType().name());
			nbtCompound.setInteger(NBTConstants.BUCKET_FLAVOUR_COUNT, flavorIngredient.getAmount());
		}

		// Alcohol
		if(alcoholIngredient != null)
		{
			nbtCompound.setString(NBTConstants.BUCKET_ALCOHOL_STRING, alcoholIngredient.getType().name());
			nbtCompound.setInteger(NBTConstants.BUCKET_ALCOHOL_COUNT, alcoholIngredient.getAmount());
		}

		return nbti.getItem();
	}

	private ArrayList<String> getFlavorText()
	{
		// Primary
		String flavourText = "A bucket of mashed " + BreweryUtils.getMaterialName(primaryIngredient.getType(), true);

		// Secondary
		if(secondaryIngredient != null)
		{
			flavourText += " mixed with " + BreweryUtils.getMaterialName(secondaryIngredient.getType(), true);
		}

		// Flavor
		if(flavorIngredient != null)
		{
			flavourText += " and flavoured with " + BreweryUtils.getMaterialName(flavorIngredient.getType(), true);
		}

		flavourText += "."; // End this.

		// Alcohol
		if(alcoholIngredient != null)
		{

			flavourText += " This mash is";
			switch(alcoholIngredient.getAmount())
			{
			case 1:
				flavourText += " slightly";
				break;
			case 2:
			case 3:
				flavourText += " moderately";
				break;
			case 4:
			case 5:
				flavourText += " heavily";
				break;
			}
			flavourText += " alcoholic.";
		}

		flavourText += " There is enough material to fill " + primaryIngredient.getAmount() * 3 + " bottles."; 

		// Word Wrap
		ArrayList<String> flavourList = new ArrayList<String>();
		flavourList.addAll(BreweryUtils.wordWrap(flavourText));

		// Return
		return flavourList;
	}

	private ArrayList<String> getFermentedFlavorText()
	{
		// Primary
		IngredientData primaryData = IngredientDatabase.getIngredientData(primaryIngredient.getType());
		String flavourText = "A bucket of unfinished " + 
				(alcoholIngredient != null ? primaryData.getAlcoholicDrinkName() : primaryData.getDrinkName());

		// Flavor
		if(flavorIngredient != null)
		{
			IngredientData flavourData = IngredientDatabase.getIngredientData(flavorIngredient.getType());
			flavourText += " with a " + flavourData.getFlavorDescriptor() + " flavor";
		}

		// Add a period
		flavourText +=".";

		// Secondary
		if(secondaryIngredient != null)
		{
			IngredientData secondaryData = IngredientDatabase.getIngredientData(secondaryIngredient.getType());
			flavourText += 
					" Drinking this will grant a " 
							+ BreweryUtils.getReadbleName(secondaryData.getPotionEffectType().getName(), true)
							+ " effect.";
		}

		// Alcohol
		if(alcoholIngredient != null)
		{

			flavourText += " This drink will be";
			switch(alcoholIngredient.getAmount())
			{
			case 1:
				flavourText += " slightly";
				break;
			case 2:
			case 3:
				flavourText += " moderately";
				break;
			case 4:
			case 5:
				flavourText += " heavily";
				break;
			}
			flavourText += " alcoholic.";
		}

		flavourText += " There is enough material to fill " + primaryIngredient.getAmount() * 3 + " bottles."; 

		// Word Wrap
		ArrayList<String> flavourList = new ArrayList<String>();
		flavourList.addAll(BreweryUtils.wordWrap(flavourText));

		// Return
		return flavourList;
	}

	private String getName()
	{
		String name = "Bucket of Mashed " + BreweryUtils.getMaterialName(primaryIngredient.getType(), false);
		return name;
	}

	private String getFermentedName()
	{
		String name = "";
		IngredientData data = IngredientDatabase.getIngredientData(primaryIngredient.getType());
		if(data != null)
		{
			name = "Bucket of Unfinished " + (alcoholIngredient != null ? data.getAlcoholicDrinkName() : data.getDrinkName());
		}
		else
		{
			name = "Bucket of Unknown Fermented Mash";
		}
		return name;
	}
}
