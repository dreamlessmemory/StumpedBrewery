package com.dreamless.brewery;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.ChatPaginator;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
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
	//TODO: add potmult/durmult
	public static BRecipe getRecipe(Player player, String type, Map<String, Double> effectAspects, Map<String, Double> flavorAspects, boolean isAged, boolean isDistilled, int potencyMultiplier, int durationMultiplier){
		//Get Top 3 Effect Aspects
		AspectComparator effecAspectComparator = new AspectComparator(effectAspects);
		NavigableMap<String, Double> topEffectAspects = new TreeMap<String, Double>(effecAspectComparator);
		for(String aspect: effectAspects.keySet()) {
			topEffectAspects.put(aspect, effectAspects.get(aspect));
		}
		while(topEffectAspects.size() > 3) {
			topEffectAspects.pollLastEntry();
		}
		//Get top 6 Flavor Aspects
		AspectComparator flavorAspectComparator = new AspectComparator(flavorAspects);
		NavigableMap<String, Double> topFlavorAspects = new TreeMap<String, Double>(flavorAspectComparator);
		//Add to new tree
		for(String aspect: flavorAspects.keySet()) {
			topFlavorAspects.put(aspect, flavorAspects.get(aspect));
		}
		while(topFlavorAspects.size() > 6) {
			topFlavorAspects.pollLastEntry();
		}
		
		//Combine
		HashMap<String, Double> combinedAspects = new HashMap<String, Double>();
		combinedAspects.putAll(topFlavorAspects);
		combinedAspects.putAll(topEffectAspects);
		
		return getRecipe(player, type, combinedAspects, isAged, isDistilled, potencyMultiplier, durationMultiplier); 
	}
	//TODO: add potmult/durmult
	public static BRecipe getRecipe(Player player, String type, Map<String, Double> combinedAspects, boolean isAged, boolean isDistilled, int potencyMultiplier, int durationMultiplier){	
		//Prep the SQL
		String starterQuery = "SELECT * FROM recipes WHERE type=? AND isAged=? AND isDistilled=? AND potencymult=? AND durationmult=?";
		String aspectQuery = "";
		String aspectColumn = "' IN (aspect1name, aspect1name, aspect2name, aspect3name, aspect4name, aspect5name, aspect6name, aspect7name, aspect8name, aspect9name)";
		String fullQuery = "";
		
		for(String aspect: combinedAspects.keySet()) {
			aspectQuery = aspectQuery.concat(" AND '" + aspect + aspectColumn);
		}
		fullQuery = starterQuery + aspectQuery;
		
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(fullQuery)){
			stmt.setString(1, type);
			stmt.setBoolean(2, isAged);
			stmt.setBoolean(3, isDistilled);
			stmt.setInt(4, potencyMultiplier);
			stmt.setInt(5, durationMultiplier);
			Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {//New recipe!
				Brewery.breweryDriver.debugLog("Nothing returned? New recipe!");
				player.sendMessage(ChatColor.GREEN + "[Brewery] " + ChatColor.RESET + "You have come across a novel " + type.toLowerCase() + " brew!");	
				return generateNewRecipe(player, type, combinedAspects, isAged, isDistilled, potencyMultiplier, durationMultiplier);				
			} else {//Found something
				do {
					Brewery.breweryDriver.debugLog("Checking recipe: - " + results.getString("name"));
					if(combinedAspects.size() != results.getInt("aspectCount")) {//Not the right number of aspects
						Brewery.breweryDriver.debugLog("reject, insufficient aspects");
						continue;
					}
					boolean allAspectsFound = true; //didn't find it
					for(Map.Entry<String, Double> es :combinedAspects.entrySet()) {
						String currentAspect = es.getKey().trim();
						double aspectRating = es.getValue();
						boolean aspectFound = false;
						
						Brewery.breweryDriver.debugLog("Do you have a " + currentAspect + " @ " + aspectRating);
						
						for(int i = 1; i <= combinedAspects.size(); i++) {//Iterate through aspectNname columns
							String aspectNameColumn = "aspect" + i + "name";
							String aspectRatingColumn = "aspect" + i + "rating";
							String aspectName = results.getString(aspectNameColumn).trim();
							Brewery.breweryDriver.debugLog("checking..." + aspectName);
							if(aspectName.equalsIgnoreCase(currentAspect)){//So, it has the aspect
								int recipeRating = results.getInt(aspectRatingColumn);
								if(aspectRating >= recipeRating && aspectRating < recipeRating + 10) {//found it
									aspectFound = true;
									Brewery.breweryDriver.debugLog("You do! And correct rating");
									break;
								}
								Brewery.breweryDriver.debugLog("You do...but wrong rating. - This recipe is rated " + recipeRating);
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
							player.sendMessage(ChatColor.GREEN + "[Brewery] " + ChatColor.RESET + "You have come across a novel " + type.toLowerCase() + " brew!");
							addRecipeToClaimList(player.getUniqueId().toString(), results.getString("name"));
						}
						return new BRecipe(results.getString("name"), generateLore(results.getString("inventor"), results.getString("flavortext"), combinedAspects));
					}
				} while (results.next());			
				//If we get here, nothing was found. So make a new one?
				Brewery.breweryDriver.debugLog("None found?");
				player.sendMessage(ChatColor.GREEN + "[Brewery] " + ChatColor.RESET + "You have come across a novel " + type.toLowerCase() + " brew!");		
				return generateNewRecipe(player, type, combinedAspects, isAged, isDistilled, potencyMultiplier, durationMultiplier);
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} 
		return new BRecipe("Unknown Brew", "A strange brew with no effects...");
	}
	
	private static BRecipe generateNewRecipe(Player player, String type, Map<String, Double> aspects, boolean isAged, boolean isDistilled, double potencyMultiplier, double durationMultiplier){
		//Variables?
		
		//Get variables
		String uuid = player.getUniqueId().toString();
		String lowercaseType = type.charAt(0) + type.substring(1).toLowerCase();
		String flavor = "A novel " + lowercaseType.toLowerCase() + " brew";
		
		String newName = generateNewName(player, lowercaseType);
		ArrayList<String> newLore = generateLore(null, flavor, aspects);
		
		addRecipeToClaimList(uuid, newName);
		addRecipeToMainList(newName, type, aspects, isAged, isDistilled, flavor, potencyMultiplier, durationMultiplier);
		
		return new BRecipe(newName, newLore);
	}
	
	private static void addRecipeToMainList(String name, String type, Map<String, Double> aspects, boolean isAged, boolean isDistilled, String flavor, double potencyMultiplier, double durationMultiplier){
	
		//Build SQL
		String query = "INSERT INTO recipes ";
		String mandatoryColumns = "(name, type, isAged, isDistilled, aspectCount, flavortext, potencymult, durationmult";
		String mandatoryValues = "VALUES (?, ?, ?, ?, ?, ?, ?, ?";	
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
		//Brewery.breweryDriver.debugLog(query);
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			
			//Mandatory
			stmt.setString(1, name);
			stmt.setString(2, type);
			stmt.setBoolean(3, isAged);
			stmt.setBoolean(4, isDistilled);
			stmt.setInt(5, aspects.size());
			stmt.setString(6, flavor);
			stmt.setDouble(7, potencyMultiplier);
			stmt.setDouble(8, durationMultiplier);
			
			Brewery.breweryDriver.debugLog(stmt.toString());
			
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void addRecipeToClaimList(String uuid, String name){
		//Get time
		java.util.Date dt = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime = sdf.format(dt);
		
		//Build SQL
		String query = "INSERT INTO newrecipes (inventor, claimdate, brewname) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE inventor=inventor";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, uuid);
			stmt.setString(2, currentTime);
			stmt.setString(3, name);
			
			//Brewery.breweryDriver.debugLog(stmt.toString());
			
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	private static String generateNewName(Player player, String type){
		String name = "";
		name = type + " Brew #";
		int count = 0;
		//SQL
		String query = "SELECT COUNT(*) FROM recipes";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
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
	
	private static ArrayList<String> generateLore(String inventorUUID, String flavorText, Map<String, Double> aspects){
		ArrayList<String> flavor = new ArrayList<String>();
		
		//Add Name
		String inventorName = "an unknown brewer";;
		if(inventorUUID != null) {
			Player inventor = Bukkit.getOfflinePlayer(UUID.fromString(inventorUUID)).getPlayer();
			if(inventor != null) {
				inventorName = inventor.getDisplayName();
			}
		}
		flavor.add("First invented by " + inventorName);
		
		//Add Aspects
		flavor.addAll(Arrays.asList(ChatPaginator.wordWrap(color(convertAspects(aspects)), WRAP_SIZE)));
		
		//Add flavortext
		flavor.addAll(Arrays.asList(ChatPaginator.wordWrap(ChatColor.GRAY + flavorText, WRAP_SIZE)));
		
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

	public String getName() {
		return name;
	}

    public ArrayList<String> getFlavorText(ArrayList<String> crafters) {
    	ArrayList<String> toReturn = new ArrayList<String>();
    	toReturn.addAll(flavorText);
    	//toReturn.addAll(customFlavorText);
    	
    	//Crafters
    	String craftString = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "Crafted by ";
    	for(String crafter : crafters) {
    		craftString += crafter + ", ";
    	}
    	
    	toReturn.addAll(Arrays.asList(ChatPaginator.wordWrap(craftString.substring(0, craftString.length()-2), WRAP_SIZE)));
    	return toReturn;
	}
    
    private static String convertAspects(Map<String, Double> aspects){
    	String startup = "&o&dThis brew is";
    	String description = "";
    	String query = "SELECT description FROM aspects WHERE aspect=?";
    	for(Map.Entry<String, Double> entry: aspects.entrySet()) {
    		String aspect = entry.getKey();
    		if(aspect.contains("_DURATION") || aspect.contains("_POTENCY")) {
    			continue;
    		}
    		
    		//Grab description from table
			try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
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
    	if(rating < 20) {
    		return " a tiny bit ";
    	} else if (rating >= 20 && rating < 30) {
    		return " somewhat ";
    	} else if (rating >= 30 && rating < 50) {
    		return " moderately ";
    	} else if (rating >= 50 && rating < 80) {
    		return " very  ";
    	} else if (rating >= 80) {
    		return " supremely ";
    	} else {
    		return " sort of ";
    	}
    }
    
    private static String color(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }
  //TODO: add potmult/durmult
    public static ItemStack revealMaskedBrew(ItemStack item, String equipmentType) {
		//Pull NBT
		NBTItem nbti = new NBTItem(item);
		NBTCompound brewery = nbti.getCompound("brewery");
		NBTCompound aging = brewery.getCompound("aging");
		NBTCompound distilling = brewery.getCompound("distilling");
		
		//Remove isAging and isDistilling tags
		if(aging != null && equipmentType.equalsIgnoreCase("Barrel")) {
			aging.removeKey("isAging");
			brewery.setBoolean("finishedAging", true);
			Brewery.breweryDriver.debugLog("Removed Aging Tag");
		}
		if(distilling != null && equipmentType.equalsIgnoreCase("BrewingStand")) {
			distilling.removeKey("isDistilling");
			brewery.setBoolean("finishedDistilling", true);
			Brewery.breweryDriver.debugLog("Removed Distilling Tag");
		}
				
				
		//Pull aspects
		NBTCompound aspectBaseList = brewery.getCompound("aspectsBase");
		NBTCompound aspectActivationList = brewery.getCompound("aspectsActivation");
		Set<String> aspects = aspectBaseList.getKeys();
		HashMap <String, Double> agedAspects = new HashMap<String, Double>();
		for(String currentAspect : aspects) {
			double effectiveRating = aspectBaseList.getDouble(currentAspect) * Aspect.getEffectiveActivation(currentAspect, aspectActivationList.getDouble(currentAspect) * 100, brewery.getString("type"));
			if(currentAspect.contains("_POTENCY")) {
				effectiveRating *= brewery.getDouble("potency");
			} else if (currentAspect.contains("_DURATION")) {
				effectiveRating *= brewery.getDouble("duration");
			}
			agedAspects.put(currentAspect, effectiveRating);
			Brewery.breweryDriver.debugLog("Unmasked aspect " + currentAspect + " rating: " + effectiveRating);
		}
		
		//Get Recipe
		Player player = null;
		if(brewery.hasKey("placedInBrewer")) {
			player = Bukkit.getPlayer(UUID.fromString(brewery.getString("placedInBrewer")));
			brewery.removeKey("placedInBrewer");
			Brewery.breweryDriver.debugLog("Removed Player");
		}
		String crafterName = player.getDisplayName();
		BRecipe recipe = BRecipe.getRecipe(player, brewery.getString("type"), agedAspects, brewery.hasKey("aging"), brewery.hasKey("distilling"), brewery.getInteger("potency"), brewery.getInteger("duration"));
		
		//Handle crafter list
		ArrayList<String> craftersList = new ArrayList<String>();
		NBTCompound crafterTags = brewery.getCompound("crafters");
		for(String crafters : crafterTags.getKeys()) {
			craftersList.add(crafters);
		}
		if(player!= null && !crafterTags.hasKey(crafterName)) {
			craftersList.add(crafterName);
			crafterTags.setString(crafterName, crafterName);
		}
		
		item = nbti.getItem();
		
		
		//Get PotionMeta
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		
		//Set new effects
		ArrayList<PotionEffect> effects = BEffect.calculateEffect(agedAspects, brewery.getInteger("potency"), brewery.getInteger("duration"));
		potionMeta.clearCustomEffects();
		for (PotionEffect effect: effects) {
			potionMeta.addCustomEffect(effect, true);
		}
		potionMeta.removeItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		
		//Name
		potionMeta.setDisplayName(recipe.getName());
		
		//FlavorText
		potionMeta.setLore(recipe.getFlavorText(craftersList));

		//assign meta
		item.setItemMeta(potionMeta);
		
		return item;
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
    	String recipeQuery = "DELETE FROM recipes WHERE EXISTS (SELECT * FROM newrecipes WHERE isclaimed=false AND claimdate < ?)";
    	String newRecipeQuery = "DELETE FROM newrecipes WHERE claimdate < ?";
    	
    	//Main Recipe List
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(recipeQuery)){
			stmt.setString(1, sevenDaysAgo);
			//Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
    	//Claim Recipe List
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(newRecipeQuery)){
			stmt.setString(1, sevenDaysAgo);
			//Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
    }

    public static boolean purgeRecipes() {
    	String recipeQuery = "DELETE FROM recipes WHERE isclaimed=false";
		String newRecipeQuery = "DELETE FROM newrecipes";
    	try (PreparedStatement stmtMain = Brewery.connection.prepareStatement(recipeQuery); PreparedStatement stmtClaim = Brewery.connection.prepareStatement(newRecipeQuery)){
			stmtMain.executeUpdate();
			stmtClaim.executeUpdate();
			return true;
		} catch (SQLException e1) {
			e1.printStackTrace();
			return false;
		}

    }
    
    public static void purgePlayer(String name) {
    	//Get UUID of player
    	UUID uuid;
    	try {
			uuid = Brewery.breweryDriver.getUUID(name);
		} catch (ParseException | org.json.simple.parser.ParseException e) {
			return;
		}
    	
    	if(uuid == null) {//All attempts to get player failed
    		return;
    	}
    	
    	String uuidString = uuid.toString();
    	String queryClaim = "DELETE FROM newrecipes WHERE inventor=?";
    	String queryMain = "DELETE FROM recipes WHERE inventor=? OR NOT EXISTS (SELECT 1 FROM newrecipes WHERE newrecipes.brewname=recipes.name)";
    	
    	//Claim List
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryClaim)){
			stmt.setString(1, uuidString);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		} 
    	//Main List
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryMain)){
			stmt.setString(1, uuidString);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		} 
  
    }
    
    public static ArrayList<String> listPlayerRecipes(Player player, boolean claimed) {
    	ArrayList<String> list = new ArrayList<String>();
    	String query;
		if(claimed) {
			query = "SELECT name FROM recipes WHERE inventor=? AND NOT EXISTS (SELECT brewname FROM newrecipes WHERE recipes.name = newrecipes.brewname)";
		} else {
			query = "SELECT brewname FROM newrecipes WHERE inventor=?";
		}
    	
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){			
			stmt.setString(1, player.getUniqueId().toString());
			Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results;
			results = stmt.executeQuery();
			if(!results.next()) {
				list.add("You have no brews to your name!");
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
    	if(newName.isEmpty()) {
    		newName = player.getDisplayName() + "'s " + currentRecipe;
    	}
    	
    	//SQL
    	String queryGetClaim = "SELECT * FROM newrecipes WHERE inventor=? AND brewname=?";
    	String queryUpdateRecipeTable = "UPDATE recipes SET inventor=?, isclaimed=true, name=? WHERE name=?";
    	String queryDeleteClaims = "DELETE FROM newrecipes WHERE brewname=?";
    	
    	//Get Claim
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryGetClaim)){
			stmt.setString(1, uuid);
			stmt.setString(2, currentRecipe);
			Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results = stmt.executeQuery();
			if(!results.next()) {//Didn't find
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "You do not have any rights to claim this brew!");
				return;
			}			
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}

    	
    	//Update recipe table
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryUpdateRecipeTable)){
			//Prepare statement
			stmt.setString(1, uuid);
			stmt.setString(2, newName);
    		stmt.setString(3, currentRecipe);
			
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
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryDeleteClaims)){
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
    
    public static void relinquishRecipe(Player player) {
    	//Potion stuff
    	String uuid = player.getUniqueId().toString();
    	ItemStack item = player.getInventory().getItemInMainHand();
    	String currentRecipe = item.getItemMeta().getDisplayName();
    	NBTItem nbti = new NBTItem(item);
		NBTCompound breweryMeta = nbti.getCompound("brewery");
		String type = breweryMeta.getString("type");
    	
    	//SQL
    	String queryMainList = "DELETE FROM recipes WHERE name=? AND inventor=?";
    	String queryClaimList = "DELETE FROM newrecipes WHERE brewname=? AND inventor=?";
    	String queryPurgeClaims = "DELETE FROM recipes WHERE NOT EXISTS (SELECT 1 FROM newrecipes WHERE newrecipes.brewname=?) AND name=? AND type=?";
    	//NOT EXISTS (SELECT 1 FROM newrecipes WHERE newrecipes.brewname=recipes.name)
    	//"DELETE FROM recipes WHERE (SELECT COUNT(1) FROM newrecipes WHERE name=?) = 0 AND name=? AND type=?";
    	
    	//Delete off of main list
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryMainList)) {
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
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryClaimList)) {
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
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(queryPurgeClaims)) {
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
    
    public static void renameRecipe(Player player, String name) {
    	//Potion stuff
    	String uuid = player.getUniqueId().toString();
    	ItemStack item = player.getInventory().getItemInMainHand();
    	String currentRecipe = item.getItemMeta().getDisplayName();
    	
    	String query = "UPDATE recipes SET name=? WHERE inventor=? AND name=?";
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, name);
			stmt.setString(2, uuid);
			stmt.setString(3, currentRecipe);
			Brewery.breweryDriver.debugLog(stmt.toString());
			int result = stmt.executeUpdate();
			if(result == 0) {
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Unable to rename " + currentRecipe);
				return;
			}
			player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "All future " + currentRecipe + " will now be called " + name);
			//Rename Potion in confirmation
			PotionMeta meta = (PotionMeta) item.getItemMeta();
			meta.setDisplayName(name);
			item.setItemMeta(meta);
		} catch (SQLException e1) {
			if(e1 instanceof MySQLIntegrityConstraintViolationException) {
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "A brew with that name already exists!");
			} else {
				e1.printStackTrace();
			}
			return;
		}  
    }
    
    public static void giveRecipeFlavorText(Player player, String flavortext) {
    	//Potion stuff
    	String uuid = player.getUniqueId().toString();
    	ItemStack item = player.getInventory().getItemInMainHand();
    	String currentRecipe = item.getItemMeta().getDisplayName();
    	
    	String query = "UPDATE recipes SET flavortext=? WHERE inventor=? AND name=?";
    	try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, flavortext);
			stmt.setString(2, uuid);
			stmt.setString(3, currentRecipe);
			Brewery.breweryDriver.debugLog(stmt.toString());
			int result = stmt.executeUpdate();
			if(result == 0) {
				player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Unable to add flavor text to " + currentRecipe);
				return;
			}
			player.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.RESET + "Flavor text for " + currentRecipe + " has been added!");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}  
    }
    
    public static int getAlcohol(ItemStack item) {
		NBTItem nbti = new NBTItem(item);
		NBTCompound brewery = nbti.getCompound("brewery");
    	
    	String query = "SELECT * FROM brewflags WHERE type=?";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, brewery.getString("type"));
			//Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results = stmt.executeQuery();
			if(!results.next()) {
				return 0; //Get out, by default people don't want to be drunk
			} else {
				int calcAlc = results.getInt("alcoholmin") + (brewery.getCompound("aspects").getKeys().size() * results.getInt("alcoholstep"));
				calcAlc = Math.min(calcAlc, results.getInt("alcoholmax"));
				return calcAlc;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
    	return 0;
    }
}