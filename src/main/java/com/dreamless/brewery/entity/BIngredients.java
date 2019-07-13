package com.dreamless.brewery.entity;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

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

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.recipe.Aspect;
import com.dreamless.brewery.recipe.Aspect.AspectRarity;
import com.dreamless.brewery.recipe.BEffect;
import com.dreamless.brewery.utils.BreweryMessage;
import com.dreamless.brewery.utils.BreweryUtils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BIngredients implements InventoryHolder{

	private Inventory inventory;
	private HashMap<String, Aspect> aspectMap = new HashMap<String, Aspect>();
	private String type;
	private boolean cooking = false;
	private String coreIngredient= "";
	private String adjunctIngredient = "";
	private int coreAmount = 0;
	private int adjunctAmount = 0;

	// Init a new BIngredients
	public BIngredients() {
		inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
	}

	// Load from database
	public BIngredients(String inventoryString, HashMap<String, Aspect> aspects, int state, boolean cooking) {
		this.aspectMap = aspects;
		this.cooking = cooking;
				
		//Initialize Inventory
		//try {
		//	inventory = BreweryUtils.fromBase64(inventoryString, this);
		//} catch (IOException e) {
		//	inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
		//	Brewery.breweryDriver.debugLog("Error creating inventory for a cauldron");
		//	e.printStackTrace();
		//}
		
		//Initialize
		determineCoreAndAdjunct(inventory.getContents());
		/*for(ItemStack item: inventory.getContents()) {
			add(item);
		}*/
		calculateType(state);
		
		
	}

	public BreweryMessage startCooking(Block block) {
		ItemStack[] contents = inventory.getContents();
		for(int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if(item != null) {
		    	if(acceptableIngredient(item.getType())) {
		    		if(usesBucket(item)) {
		    			block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5), new ItemStack(Material.BUCKET));
		    			block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, (float)(Math.random()/2) + 0.75f, (float)(Math.random()/2) + 0.75f);
		    		}
		    		add(item);
		    	} else {//eject
		    		block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation(), item);
		    		block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP,(float)(Math.random()/2) + 0.75f, (float)(Math.random()/2) + 0.75f);
		    		//inventory.remove(item);
		    		contents[i] = null;
		    	}
		    }
		}
		
		inventory.setContents(contents);
		
		//Check if empty
		//if(inventory.isEmpty()) {
		if(isEmpty()) {
			return new BreweryMessage(false, Brewery.getText("Fermentation_No_Ingredients"));
		}
		
		//Set Feedback effects
		block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().getX() + 0.5, block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
		
		//Manage parameters 
		setCooking(true);
		determineCoreAndAdjunct(contents);
		
		//Calculate type
		calculateType(0);
		
		//Return
		return new BreweryMessage(true, Brewery.getText("Fermentation_Start_Fermenting") + type.toLowerCase() + ".");
	}
	
	private void determineCoreAndAdjunct(ItemStack[] contents) {
		if(contents[0] != null) {
			coreIngredient = contents[0].getType().name();
			coreAmount = contents[0].getAmount();
			if(contents[1] != null) {
				adjunctIngredient = contents[1].getType().name();
				adjunctAmount = contents[1].getAmount();
			} else {
				adjunctIngredient = "";
				adjunctAmount = 0;
			}
		} else {
			coreIngredient = "";
			coreAmount = 0;
			adjunctIngredient = "";
			adjunctAmount = 0;
		}
	}

	// Add an ingredient to this
	public void add(ItemStack ingredient) {
		// SQL
		String aspectQuery = "name, aspect1name, aspect1rating, aspect2name, aspect2rating, aspect3name, aspect3rating";
		String query = "SELECT " + aspectQuery + " FROM " + Brewery.getDatabase(null) + "ingredients WHERE name=?";
		
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
	}

	public void fermentOneStep(int state) {
		//Calculate type
		calculateType(state);
		
		//Update aspects
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

	// returns an Potion item with cooked ingredients
	public ItemStack finishFermentation(int state, Player player) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

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
	
	public void dumpContents(Block block) {
		for(ItemStack item : inventory.getContents())	{
		    if(item != null) {
		    	block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5), item);
		    	block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP,(float)(Math.random()/2) + 0.75f, (float)(Math.random()/2) + 0.75f);
		    	inventory.remove(item);
		    }
		}
	}
	
	private void calculateType(int time) {
		String result = queryForType(coreIngredient, coreAmount, adjunctIngredient, adjunctAmount, time);
		
		if(result != null) {//There is a secondary
			type = result;
		} else {
			result = queryForType(coreIngredient, 1, "", 0, 0);
			if(result == null) {
				type = "ELIXR";
			} else {
				type = result;
			}
		}
		
		Brewery.breweryDriver.debugLog("Resultant brew: " + type);
	}
	private String queryForType(String primary, int primaryAmount, String secondary, int secondaryAmount, int time){
		String result = null;
		String query = "SELECT type FROM " + Brewery.getDatabase("brewtypes") + "brewtypes WHERE core=? AND adjunct=? GROUP BY type "
				+ "HAVING MAX(coreamount) <= ? AND MAX(adjunctamount) <= ? AND MAX(time) <=? "
				+ "ORDER BY time DESC LIMIT 1";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			//Set values
			stmt.setString(1, primary);
			stmt.setString(2, secondary);
			stmt.setInt(3, primaryAmount);
			stmt.setInt(4, secondaryAmount);
			stmt.setInt(5, time);
			
			Brewery.breweryDriver.debugLog(stmt.toString());
			
			//Retrieve results
			ResultSet results;
			results = stmt.executeQuery();
			if (results.next()) {
				result = results.getString("type");
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		return result;
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

	private boolean acceptableIngredient(Material material) {
		// SQL
		String query = "SELECT EXISTS(SELECT 1 FROM " + Brewery.getDatabase(null) + "ingredients WHERE name='" + material.name() + "')";
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

	public boolean isEmpty() {
		for(ItemStack it : inventory.getContents())	{
		    if(it != null) return false;
		}
		return true;
	}

	public String getType() {
		return type;
	}

	public boolean isCooking() {
		return cooking;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
	public HashMap<String, Aspect> getAspects() {
		return aspectMap;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setCooking(boolean cooking) {
		this.cooking = cooking;
	}

	public void setAspects(HashMap<String, Aspect> aspects) {
		this.aspectMap = aspects;
	}
}