package com.dreamless.brewery.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.BreweryRecipe;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class DatabaseCommunication {
	public static BreweryRecipe getRecipe(Player player, RecipeEntry entry){
		//Prep the SQL
		String query = "SELECT * FROM " + Brewery.getDatabase(null) + "recipes WHERE effectkey=?";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, entry.generateKey());
			Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {//New recipe!
				Brewery.breweryDriver.debugLog("Nothing returned? New recipe!");
				player.sendMessage(MessageConstants.MESSAGE_HEADER_STRING + Brewery.getText("Recipe_New_Recipe"));

				BreweryRecipe newRecipe = new BreweryRecipe(player.getUniqueId().toString());

				if(Brewery.newrecipes) {
					addRecipeToClaimList(player.getUniqueId().toString(), newRecipe.getName());
				}

				return newRecipe;				
			} else {//Found something
				if(!results.getBoolean("isclaimed")){//Exists, but not claimed
					player.sendMessage(MessageConstants.MESSAGE_HEADER_STRING + Brewery.getText("Recipe_New_Recipe"));
					addRecipeToClaimList(player.getUniqueId().toString(), results.getString("name"));
				}
				return new BreweryRecipe(results.getString("name"), player.getUniqueId().toString(), results.getString("flavortext"));
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} 
		return new BreweryRecipe();
	}

	private static void addRecipeToMainList(String name, String type, Map<String, Double> aspects, boolean isAged, boolean isDistilled, String flavor, double potencyMultiplier, double durationMultiplier){

		//Build SQL
		String query = "INSERT INTO " + Brewery.getDatabase(null) + "recipes ";
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
		String query = "INSERT INTO " + Brewery.getDatabase(null) + "newrecipes (inventor, claimdate, brewname) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE inventor=inventor";

		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, uuid);
			stmt.setString(2, currentTime);
			stmt.setString(3, name);

			Brewery.breweryDriver.debugLog(stmt.toString());

			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}


	public static int getRecipeCount(){
		int count = 0;
		//SQL
		String query = "SELECT COUNT(*) FROM " + Brewery.getDatabase(null) + "recipes";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
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
		String recipeQuery = "DELETE FROM " + Brewery.getDatabase(null) + "recipes WHERE EXISTS (SELECT * FROM " + Brewery.getDatabase(null) + "newrecipes WHERE isclaimed=false AND claimdate < ?)";
		String newRecipeQuery = "DELETE FROM " + Brewery.getDatabase(null) + "newrecipes WHERE claimdate < ?";

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
		String queryClaim = "DELETE FROM " + Brewery.getDatabase(null) + "newrecipes WHERE inventor=?";
		String queryMain = "DELETE FROM " + Brewery.getDatabase(null) + "recipes WHERE inventor=? OR NOT EXISTS (SELECT 1 FROM " + Brewery.getDatabase(null) + "newrecipes WHERE " + Brewery.getDatabase(null) + "newrecipes.brewname=recipes.name)";

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
			query = "SELECT name FROM " + Brewery.getDatabase(null) + "recipes WHERE inventor=? AND NOT EXISTS (SELECT brewname FROM newrecipes WHERE " + Brewery.getDatabase(null) + " recipes.name = " + Brewery.getDatabase(null) + "newrecipes.brewname)";
		} else {
			query = "SELECT brewname FROM " + Brewery.getDatabase(null) + "newrecipes WHERE inventor=?";
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
		String queryGetClaim = "SELECT * FROM " + Brewery.getDatabase(null) + "newrecipes WHERE inventor=? AND brewname=?";
		String queryUpdateRecipeTable = "UPDATE " + Brewery.getDatabase(null) + "recipes SET inventor=?, isclaimed=true, name=? WHERE name=?";
		String queryDeleteClaims = "DELETE FROM " + Brewery.getDatabase(null) + "newrecipes WHERE brewname=?";

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
		String queryMainList = "DELETE FROM " + Brewery.getDatabase(null) + "recipes WHERE name=? AND inventor=?";
		String queryClaimList = "DELETE FROM " + Brewery.getDatabase(null) + "newrecipes WHERE brewname=? AND inventor=?";
		String queryPurgeClaims = "DELETE FROM " + Brewery.getDatabase(null) + "recipes WHERE NOT EXISTS (SELECT 1 FROM " + Brewery.getDatabase(null) + "newrecipes WHERE " + Brewery.getDatabase(null) + "newrecipes.brewname=?) AND name=? AND type=?";
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

		String query = "UPDATE " + Brewery.getDatabase(null) + "recipes SET name=? WHERE inventor=? AND name=?";
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

		String query = "UPDATE " + Brewery.getDatabase(null) + "recipes SET flavortext=? WHERE inventor=? AND name=?";
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

		String query = "SELECT * FROM " + Brewery.getDatabase(null) + "brewflags WHERE type=?";
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