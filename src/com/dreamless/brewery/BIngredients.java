package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import com.dreamless.brewery.Aspect.AspectRarity;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class BIngredients implements InventoryHolder{

	private Inventory inventory;
	private ArrayList<ItemStack> ingredients = new ArrayList<ItemStack>();
	private HashMap<String, Aspect> aspectMap = new HashMap<String, Aspect>();
	private String type;
	private boolean cooking = false;
	private String primary= "";
	private String secondary = "";
	// Represents ingredients in Cauldron, Brew
	// Init a new BIngredients
	public BIngredients() {
		inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
	}

	// Load from File
	public BIngredients(ArrayList<ItemStack> ingredients, HashMap<String, Aspect> aspects, String type) {
		this.ingredients = ingredients;
		this.aspectMap = aspects;
		this.type = type;
				
		//Initialize Inventory
		inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
		for(ItemStack item: ingredients) {
			inventory.addItem(item);
		}
		this.primary = ingredients.get(0).getType().name();
		if(ingredients.size() > 1) {
			this.secondary = ingredients.get(1).getType().name();
		}
	}

	// Add an ingredient to this
	public boolean add(ItemStack ingredient) {
		boolean duplicate = false;
		// SQL
		String aspectQuery = "name, aspect1name, aspect1rating, aspect2name, aspect2rating, aspect3name, aspect3rating";
		String query = "SELECT " + aspectQuery + " FROM " + Brewery.database + "ingredients WHERE name=?";

		// Add Item
		int ingPosition = getIndexOf(ingredient);
		if (ingPosition != -1) {
			ingredients.get(ingPosition).setAmount(ingredients.get(ingPosition).getAmount() + ingredient.getAmount());
			duplicate = true;
		} else {
			ingredients.add(ingredient);
		}
		
		// Aspect multipliers
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setString(1, ingredient.getType().name());
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("Failed to poll SQL");
			} else {// Successful Pull
				for(int i = 1; i <=3; i++) {
					String name = results.getString("aspect"+i+"name");
					if(name == null) continue;
					Aspect aspect = aspectMap.get(name);
					int multiplier = ingredient.getAmount();
					AspectRarity values = Aspect.getRarityValues(results.getInt("aspect"+i+"rating"));
					if (aspect != null) {// aspect is found
						aspect.setValues(values.getPotency() * multiplier + aspect.getPotency(), values.getSaturation() * multiplier + aspect.getSaturation());
					} else {
						aspectMap.put(name, new Aspect(values.getPotency() * multiplier, values.getSaturation() * multiplier));
					}
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		return duplicate;
	}

	// returns an Potion item with cooked ingredients
	public ItemStack cook(int state, Player player) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// cookedTime is always time in minutes, state may differ with number of ticks
		//cookedTime = state;

		// Calculate activation and effective potency
		HashMap<String, Double> calculatedActivation = calculateActivation();
		HashMap<String, Double> calculatedEffectivePotency = calculateEffectivePotency(calculatedActivation);
		selectAspects(calculatedActivation, calculatedEffectivePotency);
		
		// Add custom potion effects based on effect aspects
		ArrayList<PotionEffect> effects = BEffect.calculateEffect(new HashMap<String, Double>(calculatedEffectivePotency), 100, 100);
		for (PotionEffect effect : effects) {
			potionMeta.addCustomEffect(effect, true);
		}

		// Recipe
		BRecipe recipe = BRecipe.getRecipe(player, type, calculatedEffectivePotency, false, false, 100, 100);
		potionMeta.setDisplayName(recipe.getName());
		ArrayList<String> craftersList = new ArrayList<String>();
		craftersList.add(player.getDisplayName());
		potionMeta.setLore(recipe.getFlavorText(craftersList));
		potionMeta.setColor(BRecipe.getColor(type));

		potion.setItemMeta(potionMeta);

		// Custom NBT Setup
		NBTItem nbti = new NBTItem(potion);
		NBTCompound breweryMeta = nbti.addCompound("brewery"); // All brewery NBT gets set here.
		
		// Aspects
		NBTCompound aspectTagList = breweryMeta.addCompound("aspectsBase");
		NBTCompound aspectActList = breweryMeta.addCompound("aspectsActivation");
		for(Entry<String, Double> entry: calculatedActivation.entrySet()) {
			aspectTagList.setDouble(entry.getKey(), aspectMap.get(entry.getKey()).getCookedBase());
			aspectActList.setDouble(entry.getKey(), entry.getValue());
		}

		// Multipliers
		breweryMeta.setInteger("potency", 100);
		breweryMeta.setInteger("duration", 100);

		// Type
		breweryMeta.setString("type", type);

		// Crafter
		NBTCompound crafters = breweryMeta.addCompound("crafters");
		crafters.setString(player.getDisplayName(), player.getDisplayName());
		
		//Finish writing NBT
		potion = nbti.getItem();

		return potion;
	}
	
	private HashMap<String, Double> calculateActivation() {
		HashMap<String, Double> activation = new HashMap<String, Double>();
		// Add calculated aspects to the map
		for (String currentAspect : aspectMap.keySet()) {
			Aspect aspect = aspectMap.get(currentAspect);
			double effectiveActivation = Aspect.getEffectiveActivation(currentAspect, aspect.getActivation(), type);
			activation.put(currentAspect, effectiveActivation);
			Brewery.breweryDriver.debugLog("PUT EFFECTIVE ACTIVATION" + currentAspect + " " + effectiveActivation);
		}
		return activation;
	}
	
	private HashMap<String, Double> calculateEffectivePotency(HashMap<String, Double> activation) {
		HashMap<String, Double> effective = new HashMap<String, Double>();
		for(Entry<String, Double> entry: activation.entrySet()) {
			effective.put(entry.getKey(), aspectMap.get(entry.getKey()).getCookedBase() * entry.getValue());
		}
		return effective;
	}

	private void selectAspects(HashMap<String, Double> activation, HashMap<String, Double> potency) {
		TreeMap<String, Double> potionActivation = new TreeMap<String, Double>();
		TreeMap<String, Double> flavorActivation = new TreeMap<String, Double>();
		
		// Add calculated aspects to the map
		for (String currentAspect : potency.keySet()) {		
			double rating = potency.get(currentAspect);			
			if (currentAspect.contains("_DURATION") || currentAspect.contains("_POTENCY")) {
				potionActivation.put(currentAspect, rating);
			} else {
				flavorActivation.put(currentAspect, rating);
			}
		}
	
		// Remove lowest effects
		while (potionActivation.size() > 3) {
			String victim = potionActivation.lastKey();
			potionActivation.remove(victim);
			activation.remove(victim);
			potency.remove(victim);
			Brewery.breweryDriver.debugLog("KNOCKOUT " + victim);
		}
		while (flavorActivation.size() > 6) {
			String victim = flavorActivation.lastKey();
			flavorActivation.remove(victim);
			activation.remove(victim);
			potency.remove(victim);
			Brewery.breweryDriver.debugLog("KNOCKOUT " + victim);
		}
	}

	public void fermentOneStep(int state) {
		for (String currentAspect : aspectMap.keySet()) {
			Aspect aspect = aspectMap.get(currentAspect);

			double activationIncrease = Aspect.getFermentationIncrease(state, currentAspect, type);
			double newActivation = aspect.getActivation() + activationIncrease;
			Brewery.breweryDriver.debugLog("Update Activation of " + currentAspect + ": " + aspect.getActivation()
					+ " + " + activationIncrease + " -> " + newActivation);
			aspect.setActivation(newActivation);
			aspectMap.put(currentAspect, aspect);
		}
	}

	public String getContents() {
		if(ingredients.isEmpty()) {
			return "nothing.";
		}
		String manifest = " ";
		for (ItemStack item : ingredients) {
			String itemName = "";
			for (String part : item.getType().toString().split("_")) {
				itemName = itemName.concat(part.substring(0, 1) + part.substring(1).toLowerCase() + " ");
			}
			manifest = manifest.concat(itemName + "x" + item.getAmount() + " - ");
		}
		return manifest.substring(0, manifest.length() - 3);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void calculateType(int time) {
		HashMap<String, Integer> resultsMap = queryForType(primary, secondary, time);
		int highestCount = 0;
		
		if(!resultsMap.isEmpty()) {//There is a secondary
			for(Entry<String, Integer> entry: resultsMap.entrySet()) {
				if(entry.getValue() >= highestCount) {
					highestCount = entry.getValue();
					type = entry.getKey();
				}
			}
		} else {
			resultsMap = queryForType(primary, "", 0);
			if(resultsMap.isEmpty()) {
				type = "ELIXR";
			} else {
				type = (String) resultsMap.keySet().iterator().next();
			}
		}
		
		Brewery.breweryDriver.debugLog("Starting brew: " + type);
	}
	
	private HashMap<String, Integer> queryForType(String primary, String secondary, int time){
		HashMap<String, Integer> resultsMap = new HashMap<String, Integer>();
		
		String query = "SELECT * FROM " + Brewery.database + "brewtypes_test WHERE core=? AND secondary=? AND time<=?";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			//Set values
			stmt.setString(1, primary);
			stmt.setString(2, secondary);
			stmt.setInt(3, time);
			
			//Retrieve results
			ResultSet results;
			results = stmt.executeQuery();
			while (results.next()) {
				resultsMap.put(results.getString("type"), results.getInt("time"));
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		return resultsMap;
	}

	public BreweryMessage startCooking(Block block) {	
		for(ItemStack item : inventory.getContents())	{
		    if(item != null) {
		    	if(BIngredients.acceptableIngredient(item.getType())) {
		    		if(usesBucket(item)) {
		    			block.getWorld().dropItem(block.getRelative(BlockFace.UP).getLocation(), new ItemStack(Material.BUCKET));
		    			block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
		    		}
		    		if(add(item)) {
		    			inventory.remove(item);
		    		}
		    	} else {//eject
		    		block.getWorld().dropItem(block.getRelative(BlockFace.UP).getLocation(), item);
		    		block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
		    		inventory.remove(item);
		    	}
		    }
		}
		if(ingredients.isEmpty()) {
			return new BreweryMessage(false, "No items were acceptable ingredients!");
		}
		
		//Set Feedback effects
		block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().getX() + 0.5, block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
		
		//Manage parameters 
		setCooking(true);
		primary = ingredients.get(0).getType().name();
		if(ingredients.size() > 1) {
			secondary = ingredients.get(1).getType().name();
		}
		
		//Calculate type
		calculateType(0);
		
		//Return
		return new BreweryMessage(true, "The cauldron begins to ferment a new " + type.toLowerCase() + ".");
	}

	public static boolean acceptableIngredient(Material material) {
		// SQL
		String query = "SELECT EXISTS(SELECT 1 FROM " + Brewery.database + "ingredients WHERE name='" + material.name() + "')";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				return false;
			} else {
				return results.getInt(1) == 1;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	public HashMap<String, Aspect> getAspects() {
		return aspectMap;
	}

	public void setAspects(HashMap<String, Aspect> aspects) {
		this.aspectMap = aspects;
	}

	public ArrayList<ItemStack> getIngredients() {
		return ingredients;
	}

	public void setIngredients(ArrayList<ItemStack> ingredients) {
		this.ingredients = ingredients;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
	
	public void dumpContents(Block block) {
		for(ItemStack item : inventory.getContents())	{
		    if(item != null) {
		    	block.getWorld().dropItem(block.getLocation(), item);
		    	inventory.remove(item);
		    }
		}
	}
	
	public boolean isEmpty() {
		for(ItemStack it : inventory.getContents())	{
		    if(it != null) return false;
		}
		return true;
	}

	public boolean isCooking() {
		return cooking;
	}

	public void setCooking(boolean cooking) {
		this.cooking = cooking;
	}
	
	private int getIndexOf(ItemStack item) {
		for (ItemStack i : ingredients) {
			if (item.isSimilar(i)) {
				return ingredients.indexOf(i);
			}
		}
		return -1;
	}
	private boolean usesBucket(ItemStack item) {
		switch(item.getType()) {
		case LAVA_BUCKET:
		case MILK_BUCKET:
		case WATER_BUCKET:
			return true;
		default:
			return false;
		}
	}
}