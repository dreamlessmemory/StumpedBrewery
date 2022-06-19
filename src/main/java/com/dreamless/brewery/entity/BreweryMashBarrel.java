package com.dreamless.brewery.entity;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.IngredientDatabase;
import com.dreamless.brewery.brew.MashBucket;
import com.dreamless.brewery.data.MessageConstants;
import com.dreamless.brewery.utils.BreweryUtils;

public class BreweryMashBarrel {

	public static void getMashBucket(Block block, Player player)
	{
		Inventory inventory = ((Container)block.getState()).getInventory();
		
		ArrayList<ItemStack> dropList = new ArrayList<ItemStack>();

		// Validate
		if(!validate(inventory, player))
		{
			// Exit and do nothing
			return;
		}
		
		// Create mash barrel
		MashBucket mashBucket = new MashBucket(inventory.getItem(1),inventory.getItem(2),inventory.getItem(3),inventory.getItem(4), player);
		ItemStack mashBucketItem = mashBucket.getItem();
		dropList.add(mashBucketItem);
		
		// Clear out spaces
		for (int index = 0; index <= 4; index++)
		{
			ItemStack item = inventory.getItem(index);
			if(item == null)
			{
				continue;
			}
			
			// Add an empty bucket if needed
			if(index != 0 && BreweryUtils.usesBucket(item))
			{
				dropList.add(new ItemStack(Material.BUCKET));
			}
			
			inventory.setItem(index, null);
		}
		
		// Effects
		block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().getX() + 0.5,
				block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
		block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_LAND, 2.0f, 1.0f);
		
		// Inform player
		player.sendMessage(MessageConstants.MESSAGE_HEADER_STRING + "You created a " + mashBucketItem.getItemMeta().getDisplayName());

		// Drop everything relevant
		Location dropLocation = block.getRelative(((Directional)block.getBlockData()).getFacing()).getLocation().add(0.5, 0.5, 0.5);
		for (ItemStack item : dropList) {
			if (item != null) {
				dropLocation.getWorld().dropItemNaturally(dropLocation, item);
				dropLocation.getWorld().playSound(dropLocation, Sound.ENTITY_ITEM_PICKUP,
						(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
			}
		}
	}

	private static boolean validate(Inventory inventory, Player player)
	{
		// Water Bucket
		if(inventory.getItem(0) == null ||
				inventory.getItem(0).getType() != Material.WATER_BUCKET)
		{
			Brewery.breweryDriver.msg(player, "You need a water bucket in the first slot!");
			return false;
		}

		// Primary
		if(inventory.getItem(1) != null &&
				!IngredientDatabase.isIngredient(inventory.getItem(1).getType()))
		{
			Brewery.breweryDriver.msg(player, "You need a valid primary ingredient in the second slot!");
			return false;
		}

		// Secondary
		if(inventory.getItem(2) != null &&
				!IngredientDatabase.isIngredient(inventory.getItem(2).getType()))
		{
			Brewery.breweryDriver.msg(player, "You need a valid secondary ingredient in the third slot!");
			return false;
		}

		// Flavor
		if(inventory.getItem(3) != null &&
				!IngredientDatabase.isIngredient(inventory.getItem(3).getType()))
		{
			Brewery.breweryDriver.msg(player, "You need a valid flavor ingredient in the fourth slot!");
			return false;
		}

		// Alcohol
		if(inventory.getItem(4) != null &&
				IngredientDatabase.getAlcoholicIngredient() != inventory.getItem(4).getType())
		{
			Brewery.breweryDriver.msg(player, "You need a valid alcoholic ingredient in the fifth slot!");
			return false;
		}
		
		return true;
	}
}
