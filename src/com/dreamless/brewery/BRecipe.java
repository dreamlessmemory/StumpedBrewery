package com.dreamless.brewery;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.ChatPaginator;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

public class BRecipe {
	
	
	//Static
	//public static ArrayList<BRecipe> recipes = new ArrayList<BRecipe>();
	private static final int WRAP_SIZE = 30;
	
	private String name = "TEST";
    private ArrayList<String> flavorText = new ArrayList<String>();
    
	public BRecipe(String name, String flavorText) {
		this.name = name;
		this.flavorText.add(flavorText);
	}
	
	public BRecipe(String name, ArrayList<String> flavorText) {
		this.name = name;
		this.flavorText = flavorText;
	}
	
	public static BRecipe getRecipe(Player player, String type, Map<String, Double> aspects, boolean isAged, boolean isDistilled) {
		AspectComparator aspectComparator = new AspectComparator(aspects);
		NavigableMap<String, Double> topAspects = new TreeMap<String, Double>(aspectComparator);
		
		for(String aspect: aspects.keySet()) {
			topAspects.put(aspect, aspects.get(aspect));
		}
		
		//Pare out the top aspects until you have the top 6
		while(topAspects.size() > 6) {
			topAspects.pollLastEntry();
		}
		
		//Prep the SQL
		String starterQuery = "SELECT * FROM recipes WHERE type=? AND isAged=? AND isDistilled=?";
		String aspectQuery = "";
		String aspectColumn = "' IN (aspect1name, aspect1name, aspect2name, aspect3name, aspect4name, aspect5name, aspect6name)";
		String fullQuery = "";
		
		for(String aspect: topAspects.keySet()) {
			aspectQuery = aspectQuery.concat(" AND '" + aspect + aspectColumn);
		}
		fullQuery = starterQuery + aspectQuery;
		Brewery.breweryDriver.debugLog(fullQuery);
		
		try {
			//boolean found = false;
			
			
			//SQL Block
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(fullQuery);
			stmt.setString(1, type);
			stmt.setBoolean(2, isAged);
			stmt.setBoolean(3, isDistilled);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {//New recipe!
				Brewery.breweryDriver.debugLog("Nothing returned? New recipe!");
				return generateNewRecipe(player, type, aspects, isAged, isDistilled);
			} else {//Found something
				do {//something
					
					Brewery.breweryDriver.debugLog("Checking recipe: - " + results.getString("name"));
					
					if(topAspects.size() != results.getInt("aspectCount")) {//Not the right number of aspects
						Brewery.breweryDriver.debugLog("reject, insufficient aspects");
						continue;
					}
					boolean allAspectsFound = true; //didn't find it
					
					//for(String currentAspect: topAspects.keySet()) {//iterate through the top aspects
					for(Map.Entry<String, Double> es :topAspects.entrySet()) {
						String currentAspect = es.getKey().trim();
						double aspectRating = es.getValue();
						boolean aspectFound = false;
						
						Brewery.breweryDriver.debugLog("Do you have a " + currentAspect);
						
						for(int i = 0; i < topAspects.size(); i++) {//Iterate through aspectNname columns
							int column = 6 + (2 * i);
							String aspectName = results.getString(column).trim();
							Brewery.breweryDriver.debugLog("checking..." + aspectName);
							if(aspectName.equalsIgnoreCase(currentAspect)){//So, it has the aspect
								int recipeRating = results.getInt(6 + 1 + (2 * i));
								//double aspectRating = topAspects.get(currentAspect); 
								if(aspectRating >= recipeRating && aspectRating < recipeRating + 9) {//found it
									aspectFound = true;
									Brewery.breweryDriver.debugLog("You do!");
									break;
								}
							}
						}
						if(!aspectFound) {//The aspect wasn't here, so not the right one. Stop looking
							Brewery.breweryDriver.debugLog("You don't!");
							allAspectsFound = false;
							break;
						}
					}
					if(allAspectsFound) {//We found it!
						Brewery.breweryDriver.debugLog("Found you!");
						return new BRecipe(results.getString("name"), generateLore(results.getString("inventor"), results.getString("flavortext"), topAspects));
					}
					
				} while (results.next());
				
				
				//If we get here, nothing was found. So make a new one?
				Brewery.breweryDriver.debugLog("None found?");
				return generateNewRecipe(player, type, aspects, isAged, isDistilled);
				
				
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		
		return new BRecipe("Unknown Brew", "A strange brew with no effects...");
	}
	
	private static BRecipe generateNewRecipe(Player player, String type, Map<String, Double> aspects, boolean isAged, boolean isDistilled) {
		//Variables?
		
		//Get variables
		String uuid = player.getUniqueId().toString();
		String lowercaseType = type.charAt(0) + type.substring(1).toLowerCase();
		String flavor = "A novel " + lowercaseType + " brew by " + player.getDisplayName();
		
		String newName = generateNewName(player, lowercaseType);
		ArrayList<String> newLore = generateLore(uuid, flavor, aspects);
		
		addRecipeToMainList(newName, uuid, type, aspects, isAged, isDistilled, flavor);
		addRecipeToClaimList(uuid, newName);
		
		//Announce?
		Bukkit.broadcastMessage(ChatColor.GREEN + "[Brewery] " + ChatColor.RESET + player.getDisplayName() + " has just invented a new " + type.toLowerCase() + " brew!");
		
		
		return new BRecipe(newName, newLore);
	}
	
	private static void addRecipeToMainList(String name, String uuid, String type, Map<String, Double> aspects, boolean isAged, boolean isDistilled, String flavor) {
	
		//Build SQL
		String query = "INSERT INTO recipes ";
		String mandatoryColumns = "(name, type, isAged, isDistilled, aspectCount, inventor, flavortext";
		String mandatoryValues = "VALUES (?, ?, ?, ?, ?, ?, ?";	
		String aspectColumns = "";
		String aspectValues = "";
		int count = 1;
	
		for(Map.Entry<String, Double> entry : aspects.entrySet()) {
			String nameColumn = "aspect" + count + "name";
			String valueColumn = "aspect" + count + "rating";
			String ratingValue = Integer.toString((int)((entry.getValue())/10) * 10);
			
			aspectColumns += ", " + nameColumn + ", " + valueColumn;
			aspectValues += ", '" + entry.getKey() + "', " + ratingValue;
			count++;
		}
		
		query += mandatoryColumns + aspectColumns + ") " + mandatoryValues + aspectValues + ")";
		Brewery.breweryDriver.debugLog(query);
		
		//TODO Aspects
		try {
			//SQL Replacement
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			
			//Mandatory
			stmt.setString(1, name);
			stmt.setString(2, type);
			stmt.setBoolean(3, isAged);
			stmt.setBoolean(4, isDistilled);
			stmt.setInt(5, aspects.size());
			stmt.setString(6, uuid);
			stmt.setString(7, flavor);
			
			Brewery.breweryDriver.debugLog(stmt.toString());
			
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void addRecipeToClaimList(String uuid, String name) {
		//Build SQL
		String query = "INSERT INTO newrecipes (inventor, claimnumber, brewname) VALUES (?, ?, ?)";
		int count = countOwnedRecipes(Bukkit.getPlayer(UUID.fromString(uuid)), "newrecipes") + 1;
		
		try {
			//SQL Replacement
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			
			//Mandatory
			stmt.setString(1, uuid);
			stmt.setInt(2, count);
			stmt.setString(3, name);
			
			Brewery.breweryDriver.debugLog(stmt.toString());
			
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	private static String generateNewName(Player player, String type) {
		String name = "";
		//String uuid = player.getUniqueId().toString();
		name += player.getDisplayName() + "'s " + type + " #";
		int count = countOwnedRecipes(player, "recipes") + 1;
		return name + count;
	}
	
	private static int countOwnedRecipes(Player player, String table) {
		String uuid = player.getUniqueId().toString();
		int count = 0;
		//SQL
		String query = "SELECT COUNT(*) FROM " + table + " WHERE inventor=?";
		//TODO Aspects
		try {
			//SQL Block
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, uuid);
			ResultSet results;
			results = stmt.executeQuery();
			if (results.next()) {
				count = results.getInt(1);
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return count;
	}
	
	public static Color getColor(String type) {
		
		try {
			String query = "SELECT color FROM brewtypes WHERE type=?";
			//SQL Block
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, type);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {//Error, so default
				return Color.fromRGB(8441558);
			} else {//Found something
				return Color.fromRGB(results.getInt(1));
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		return Color.fromRGB(8441558);
	}
	
	private static ArrayList<String> generateLore(String uuid, String flavorText, Map<String, Double> aspects){
		ArrayList<String> flavor = new ArrayList<String>();
		
		//Add Name
		String inventorName;
		Player player = Bukkit.getPlayer(UUID.fromString(uuid));
		if(player != null) {
			inventorName = player.getDisplayName();
		} else {
			inventorName = "an unknown brewer";
		}
		flavor.add("Invented by: " + inventorName);
		
		//Add Aspects
		flavor.addAll(Arrays.asList(ChatPaginator.wordWrap(color(convertAspects(aspects)), WRAP_SIZE)));
		
		//Add flavortext
		flavor.addAll(Arrays.asList(ChatPaginator.wordWrap(ChatColor.RESET + flavorText, WRAP_SIZE)));
		
		return flavor;
	}
	
	
	public boolean hasFlavorText(){
        return flavorText == null;
    }
    

	// Create a Potion from this Recipe with best values. Quality can be set, but will reset to 10 if put in a barrel
	public ItemStack create(int quality) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		potionMeta.setDisplayName(Brewery.breweryDriver.color("&f" + getName()));
		// This effect stores the UID in its Duration
		potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect(2000, 0), true);

		//brew.convertLore(potionMeta, false);
		//Brew.addOrReplaceEffects(potionMeta, effects, quality);
		
		potion.setItemMeta(potionMeta);
		return potion;
	}


	// Getter

	// name that fits the quality
	public String getName() {
		return name;
	}

    public ArrayList<String> getFlavorText() {
    	return flavorText;
	}
    
    private static String convertAspects(Map<String, Double> aspects) {
    	String startup = "&o&dThis brew is";
    	String description = "";
    	for(Map.Entry<String, Double> entry: aspects.entrySet()) {
    		String aspect = entry.getKey();
    		if(aspect.contains("_DURATION") || aspect.contains("_POTENCY")) {
    			continue;
    		}
    		
    		//Grab description from table
			try {
				String query = "SELECT description FROM aspects WHERE name='" + aspect + "'";
				//SQL Block
				PreparedStatement stmt;
				stmt = Brewery.connection.prepareStatement(query);
			
				ResultSet results;
				results = stmt.executeQuery();
				if (!results.next()) {//Can't find it, move on
					continue;
				} else {
				description += getDescriptor(entry.getValue()) + results.getString(1) + ",";
				}  
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	if(description.length() > 0) {
    		return startup + description.substring(0, description.length() - 1) + ".";
    	} else {
    		return "";
    	}
    }
    
    private static String getDescriptor(double rating) {
    	if(rating < 40) {
    		return " a tiny bit ";
    	} else if (rating >= 40 && rating < 80) {
    		return " somewhat ";
    	} else if (rating >= 80 && rating < 120) {
    		return " moderately ";
    	} else if (rating >= 120 && rating < 160) {
    		return " somewhat ";
    	} else if (rating >= 160 && rating < 200) {
    		return " somewhat ";
    	} else if (rating >= 200) {
    		return " supremely ";
    	} else {
    		return " sort of ";
    	}
    }
    
    private static String color(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /*private static List<String> color(List<String> lore){
        List<String> clore = new ArrayList<>();
        for(String s : lore){
            clore.add(color(s));
        }
        return clore;
    }*/
    
    
    private static String convertUUIDString(String uuid) {
    	return uuid.substring(0,  8) + "-" + uuid.substring(8,  12) + "-" + uuid.substring(12,  16) + "-" + uuid.substring(16,  20) + "-" + uuid.substring(20);
    }

}