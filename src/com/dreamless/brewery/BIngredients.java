package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class BIngredients {

	private ArrayList<ItemStack> ingredients = new ArrayList<ItemStack>();
	private HashMap<String, Aspect> potency = new HashMap<String, Aspect>();
	private int cookedTime;
	private String type;

	// Represents ingredients in Cauldron, Brew
	// Init a new BIngredients
	public BIngredients() {
	}

	// Load from File
	public BIngredients(ArrayList<ItemStack> ingredients, HashMap<String, Aspect> aspects, int cookedTime,
			String type) {
		this.ingredients = ingredients;
		this.potency = aspects;
		this.cookedTime = cookedTime;
		this.type = type;
	}

	// Add an ingredient to this
	public void add(ItemStack ingredient) {
		// SQL
		String aspectQuery = "name, aspect1name, aspect1rating, aspect2name, aspect2rating, aspect3name, aspect3rating";
		String query = "SELECT " + aspectQuery + " FROM ingredients WHERE name=?";

		// Add Item
		int ingPosition = getIndexOf(ingredient);
		if (ingPosition != -1) {
			ingredients.get(ingPosition).setAmount(ingredients.get(ingPosition).getAmount() + ingredient.getAmount());
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
					Aspect aspect = potency.get(name);
					double[] values = Aspect.getRarityValues(results.getInt("aspect"+i+"rating"));
					if (aspect != null) {// aspect is found
						aspect.setValues(values[0] + aspect.getPotency(), values[1] + aspect.getSaturation());
					} else {
						potency.put(name, new Aspect(values[0], values[1]));
					}
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	// returns an Potion item with cooked ingredients
	public ItemStack cook(int state, Player player) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// cookedTime is always time in minutes, state may differ with number of ticks
		cookedTime = state;

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
			aspectTagList.setDouble(entry.getKey(), potency.get(entry.getKey()).getCookedBase());
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
		for (String currentAspect : potency.keySet()) {
			Aspect aspect = potency.get(currentAspect);
			double effectiveActivation = Aspect.getEffectiveActivation(currentAspect, aspect.getActivation(), type);
			activation.put(currentAspect, effectiveActivation);
			Brewery.breweryDriver.debugLog("PUT EFFECTIVE ACTIVATION" + currentAspect + " " + effectiveActivation);
		}
		return activation;
	}
	
	private HashMap<String, Double> calculateEffectivePotency(HashMap<String, Double> activation) {
		HashMap<String, Double> effective = new HashMap<String, Double>();
		for(Entry<String, Double> entry: activation.entrySet()) {
			effective.put(entry.getKey(), potency.get(entry.getKey()).getCookedBase() * entry.getValue());
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
		for (String currentAspect : potency.keySet()) {
			Aspect aspect = potency.get(currentAspect);

			double activationIncrease = Aspect.getFermentationIncrease(state, currentAspect, type);
			double newActivation = aspect.getActivation() + activationIncrease;
			Brewery.breweryDriver.debugLog("Update Activation of " + currentAspect + ": " + aspect.getActivation()
					+ " + " + activationIncrease + " -> " + newActivation);
			aspect.setActivation(newActivation);
			potency.put(currentAspect, aspect);
		}
	}

	public int getCookedTime() {
		return cookedTime;
	}

	public String getContents() {
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

	public void calculateType() {
		// SQL
		String query = "SELECT * FROM brewtypes WHERE material=?";
		int highestCount = 0;
		int priority = 0;
		for (ItemStack ingredient : ingredients) {
			// Pull from DB
			try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
				stmt.setString(1, ingredient.getType().name());
				ResultSet results;
				results = stmt.executeQuery();
				if (results.next()) {
					if (ingredient.getAmount() > highestCount && results.getInt("priority") > priority) {
						highestCount = ingredient.getAmount();
						priority = results.getInt("priority");
						type = results.getString("type");
					}
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		if (highestCount == 0) {// no one is the right one
			type = "ELIXIR";
		}
		Brewery.breweryDriver.debugLog("Starting brew: " + type);
	}

	public void startCooking() {
		calculateType();
	}

	public static boolean acceptableIngredient(Material material) {
		// SQL
		String query = "SELECT EXISTS(SELECT 1 FROM ingredients WHERE name='" + material.name() + "')";
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
		return potency;
	}

	public void setAspects(HashMap<String, Aspect> aspects) {
		this.potency = aspects;
	}

	public ArrayList<ItemStack> getIngredients() {
		return ingredients;
	}

	public void setIngredients(ArrayList<ItemStack> ingredients) {
		this.ingredients = ingredients;
	}

	private int getIndexOf(ItemStack item) {
		for (ItemStack i : ingredients) {
			if (item.isSimilar(i)) {
				return ingredients.indexOf(i);
			}
		}
		return -1;
	}
}