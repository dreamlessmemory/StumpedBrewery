package com.dreamless.brewery;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.ChatPaginator;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
						
						for(int i = 1; i <= topAspects.size(); i++) {//Iterate through aspectNname columns
							String aspectNameColumn = "aspect" + i + "name";
							String aspectRatingColumn = "aspect" + i + "rating";
							String aspectName = results.getString(aspectNameColumn).trim();
							Brewery.breweryDriver.debugLog("checking..." + aspectName);
							if(aspectName.equalsIgnoreCase(currentAspect)){//So, it has the aspect
								int recipeRating = results.getInt(aspectRatingColumn);
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
						if(!results.getBoolean("isclaimed")){//Exists, but not claimed
							addRecipeToClaimList(player.getUniqueId().toString(), results.getString("name"));
						}
						return new BRecipe(results.getString("name"), generateLore(results.getString("inventor"), player, results.getString("flavortext"), topAspects));
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
		String flavor = "A novel " + lowercaseType.toLowerCase() + " brew";
		
		String newName = generateNewName(player, lowercaseType);
		ArrayList<String> newLore = generateLore(null, player, flavor, aspects);
		
		addRecipeToClaimList(uuid, newName);
		addRecipeToMainList(newName, type, aspects, isAged, isDistilled, flavor);
		
		//Announce?
		Bukkit.broadcastMessage(ChatColor.GREEN + "[Brewery] " + ChatColor.RESET + player.getDisplayName() + " has just invented a new " + type.toLowerCase() + " brew!");		
		
		return new BRecipe(newName, newLore);
	}
	
	private static void addRecipeToMainList(String name, String type, Map<String, Double> aspects, boolean isAged, boolean isDistilled, String flavor) {
	
		//Build SQL
		String query = "INSERT INTO recipes ";
		String mandatoryColumns = "(name, type, isAged, isDistilled, aspectCount, flavortext";
		String mandatoryValues = "VALUES (?, ?, ?, ?, ?, ?";	
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
			stmt.setString(6, flavor);
			
			Brewery.breweryDriver.debugLog(stmt.toString());
			
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void addRecipeToClaimList(String uuid, String name) {
		//Get time
		java.util.Date dt = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime = sdf.format(dt);

		
		//Build SQL
		String query = "INSERT INTO newrecipes (inventor, claimdate, brewname) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE inventor=inventor";
		
		try {
			//SQL Replacement
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			
			//Mandatory
			stmt.setString(1, uuid);
			stmt.setString(2, currentTime);
			stmt.setString(3, name);
			
			Brewery.breweryDriver.debugLog(stmt.toString());
			
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	private static String generateNewName(Player player, String type) {
		String name = "";
		//name += player.getDisplayName() + "'s " + type + " #";
		name = type + " Brew #";
		int count = 0;
		//SQL
		String query = "SELECT COUNT(*) FROM recipes";
		try {
			//SQL Block
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			ResultSet results;
			results = stmt.executeQuery();
			if (results.next()) {
				count = results.getInt(1);
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return name + count;
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
	
	private static ArrayList<String> generateLore(String inventorUUID, Player crafter, String flavorText, Map<String, Double> aspects){
		ArrayList<String> flavor = new ArrayList<String>();
		
		//Add Name
		String inventorName = "an unknown brewer";;
		if(inventorUUID != null) {
			Player inventor = Bukkit.getOfflinePlayer(UUID.fromString(inventorUUID)).getPlayer();
		//	Brewery.breweryDriver.debugLog("Inventor: " + inventorUUID);
			//inventorName = NameFetcher.getName(inventorUUID);
			if(inventor != null) {
				inventorName = inventor.getDisplayName();
			}
		}
		flavor.add("First invented by " + inventorName);
		flavor.add("Crafted by " + crafter.getDisplayName());
		
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
				String query = "SELECT description FROM aspects WHERE name=?";
				//SQL Block
				PreparedStatement stmt;
				stmt = Brewery.connection.prepareStatement(query);
				stmt.setString(1, aspect);
				ResultSet results;
				results = stmt.executeQuery();
				if (!results.next()) {//Can't find it, move on
					continue;
				} else {
				description += getDescriptor(entry.getValue()) + results.getString(1) + ",";
				}  
			} catch (SQLException e) {
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
    
    public static void periodicPurge() {
    	Brewery.breweryDriver.debugLog("Okay, running purge...");
    	//Get time
    	java.util.Date dt = new java.util.Date(System.currentTimeMillis());
    	java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(dt);
    	cal.add(Calendar.DATE, -7);
    	String sevenDaysAgo = sdf.format(cal.getTime());
    	
    	
    	//SQL
    	try {
    		String recipeQuery = "DELETE FROM recipes WHERE EXISTS (SELECT * FROM newrecipes WHERE isclaimed=false AND claimdate < ?)";
			String newRecipequery = "DELETE FROM newrecipes WHERE claimdate < ?";
			PreparedStatement stmt;
			
			//Recipes list
			stmt = Brewery.connection.prepareStatement(recipeQuery);
			stmt.setString(1, sevenDaysAgo);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
			
			//new recipes list
			stmt = Brewery.connection.prepareStatement(newRecipequery);
			stmt.setString(1, sevenDaysAgo);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
    }

    public static boolean purgeRecipes() {
    	try {
			String recipeQuery = "DELETE FROM recipes WHERE isclaimed=false";
			String newRecipeQuery = "DELETE FROM newrecipes";
			PreparedStatement stmt;
			
			//Recipes list
			stmt = Brewery.connection.prepareStatement(recipeQuery);
			stmt.executeUpdate();
			stmt = Brewery.connection.prepareStatement(newRecipeQuery);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e1) {
			e1.printStackTrace();
			return false;
		}
    }
    
    public static ArrayList<String> listPlayerRecipes(Player player, boolean claimed) {
    	ArrayList<String> list = new ArrayList<String>();
    	
    	try {
			String query;
			if(claimed) {
				query = "SELECT name FROM recipes WHERE inventor=? AND NOT EXISTS (SELECT brewname FROM newrecipes WHERE recipes.name = newrecipes.brewname)";
			} else {
				query = "SELECT brewname FROM newrecipes WHERE inventor=?";
			}
			//Brewery.breweryDriver.debugLog(query);
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, player.getUniqueId().toString());
			Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results;
			results = stmt.executeQuery();
			if(!results.next()) {
				list.add("You don't have any brews to claim!");
			} else {
				
				list.add(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Your current list of " + (claimed ? "claimed" : "unclaimed") + " brews");
				int count = 1;
				do {
					if(claimed) {
						list.add(count++ + " - " + results.getString("name"));
					} else {
						list.add(results.getString("brewname"));
					}
				} while (results.next());
			}
			
			return list;
			
		} catch (SQLException e1) {
			list.add("Error trying to get your list");
			e1.printStackTrace();
			return list;
		}
    }

    public static void claimRecipe(Player player, String newName) {
    	String uuid = player.getUniqueId().toString();
    	String currentRecipe = player.getInventory().getItemInMainHand().getItemMeta().getDisplayName();
    	
    	//SQL
    	String query;
    	ResultSet results;
    	
    	//Get Claim
    	try {
			query = "SELECT * FROM newrecipes WHERE inventor=? AND brewname=?";
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, uuid);
			stmt.setString(2, currentRecipe);
			Brewery.breweryDriver.debugLog(stmt.toString());
			results = stmt.executeQuery();
			if(!results.next()) {//Didn't find
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "You do not have any rights to claim this brew!");
				return;
			}			
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}

    	
    	//Update recipe table
    	try {
    		if(newName.isEmpty()) {
    			query = "UPDATE recipes SET inventor=?, isclaimed=true, WHERE name=?";
    		} else {
    			query = "UPDATE recipes SET inventor=?, isclaimed=true, name=? WHERE name=?";
    		}
    		
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			
			//Prepare statement
			stmt.setString(1, uuid);
			if(newName.isEmpty()) {
				stmt.setString(2, currentRecipe);
    		} else {
    			stmt.setString(2, newName);
    			stmt.setString(3, currentRecipe);
    		}
			
			Brewery.breweryDriver.debugLog(stmt.toString());
			int updateResult = stmt.executeUpdate();
			if(updateResult == 0) {
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "No brew exists? Contant MemoryReborn!");
				return;
			} 
		} catch (SQLException e1) {
			if(e1 instanceof MySQLIntegrityConstraintViolationException) {
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "A brew with that name already exists!");
			} else {
				e1.printStackTrace();
			}
			return;
		}
    	
    	//Delete all claims in newrecipes
    	try {
			query = "DELETE FROM newrecipes WHERE brewname=?";
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, currentRecipe);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
    	player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "You've claimed " + newName + "!");
    	
    	//rename item
    	ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
    	meta.setDisplayName(newName);
    	player.getInventory().getItemInMainHand().setItemMeta(meta);
    }
    
    //TODO
    //Relinquish
    //Remove recipe with name
    //Check if there is anyone else with that claim
    //remove from recipes list if no one else claimed it
    
    public static void relinquishRecipe(Player player) {
    	//Potion stuff
    	String uuid = player.getUniqueId().toString();
    	ItemStack item = player.getInventory().getItemInMainHand();
    	String currentRecipe = item.getItemMeta().getDisplayName();
    	NBTItem nbti = new NBTItem(item);
		NBTCompound breweryMeta = nbti.getCompound("brewery");
		String type = breweryMeta.getString("type");
    	
    	//SQL
    	String query;
    	
    	//Delete off of main list
    	try {
    		query = "DELETE FROM recipes WHERE name=? AND inventor=?";
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, currentRecipe);
			stmt.setString(2, uuid);
			Brewery.breweryDriver.debugLog(stmt.toString());
			int result = stmt.executeUpdate();
			if(result > 0) {
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "You've relinquished your rights to " + currentRecipe);
				return; 
				//We return here. If no one had claimed it, then inventor is null. If there's an inventor, there's nothing on the claims list.
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		} 
    		
    	//Delete off of claims list
    	try {
    		query = "DELETE FROM newrecipes WHERE brewname=? AND inventor=?";
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, currentRecipe);
			stmt.setString(2, uuid);
			Brewery.breweryDriver.debugLog(stmt.toString());
			int result = stmt.executeUpdate();
			if(result == 0) {
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "You do not have rights to " + currentRecipe);
				return;
			}
			player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "You've relinquished your rights to " + currentRecipe);
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}  	
    	
    	//Delete off of main list if it doesn't exist in claims
    	try {
    		query = "DELETE FROM recipes WHERE (SELECT COUNT(1) FROM newrecipes WHERE name=?) = 0 AND name=? AND type=?";
			PreparedStatement stmt;
			stmt = Brewery.connection.prepareStatement(query);
			stmt.setString(1, currentRecipe);
			stmt.setString(2, currentRecipe);
			stmt.setString(3, type);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}
    }
}