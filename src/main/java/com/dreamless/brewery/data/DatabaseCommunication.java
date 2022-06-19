package com.dreamless.brewery.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.BreweryRecipe;
import java.sql.SQLIntegrityConstraintViolationException;

public class DatabaseCommunication {
	
	public static BreweryRecipe getRecipe(Player player, int recipeHash){
		//Prep the SQL
		String query = "SELECT * FROM " + Brewery.getDatabase("recipes") + "recipes WHERE id=?";

		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, String.valueOf(recipeHash));
			Brewery.breweryDriver.debugLog(stmt.toString());
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {//New recipe!
				//Inform User
				Brewery.breweryDriver.debugLog("Nothing returned? New recipe!");
				player.sendMessage(MessageConstants.MESSAGE_HEADER_STRING + Brewery.getText("Recipe_New_Recipe"));

				// Get New Recipe
				BreweryRecipe newRecipe = new BreweryRecipe(player.getUniqueId().toString());

				if(Brewery.newrecipes) {
					addRecipeToDatabase(recipeHash, newRecipe.getName(), player.getUniqueId().toString(), newRecipe.getFlavorTextString());
				}

				return newRecipe;				
			} else {//Recipe Found
				return new BreweryRecipe(results.getString(DatabaseConstants.BREW_NAME_STRING), 
						results.getString(DatabaseConstants.CLAIMANT_STRING), 
						results.getString(DatabaseConstants.FLAVORTEXT_STRING));
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		// This really shouldn't be called...
		return new BreweryRecipe("");
	}

	private static void addRecipeToDatabase(int hashCode, String brewname, String crafter, String flavortext){

		//Build SQL
		String query = "INSERT INTO " + Brewery.getDatabase("recipes") + "recipes (id, name, inventor, flavortext) VALUES (?, ?, ?, ?)";

		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){

			stmt.setString(1, String.valueOf(hashCode));
			stmt.setString(2, brewname);
			stmt.setString(3, crafter);
			stmt.setString(4, flavortext);

			Brewery.breweryDriver.debugLog(stmt.toString());

			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	public static int getRecipeCount(){
		int count = 0;
		String query = "SELECT COUNT(*) FROM " + Brewery.getDatabase("recipes") + "recipes";
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
		String query = "DELETE FROM " + Brewery.getDatabase("recipes") + "recipes WHERE inventor=?";
		//Claim List
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
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
			query = "SELECT name FROM " + Brewery.getDatabase("recipes") + 
					"recipes WHERE inventor=? AND NOT EXISTS (SELECT effectkey FROM " + Brewery.getDatabase("recipes") + "newrecipes WHERE " +
					Brewery.getDatabase("recipes") + "recipes.effectkey = " + Brewery.getDatabase("recipes") + "newrecipes.effectkey)";
		} else {
			query = "SELECT name FROM " + Brewery.getDatabase("recipes") + "newrecipes WHERE inventor=?";
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
						list.add(count++ + " - " + results.getString(DatabaseConstants.BREW_NAME_STRING));
					} else {
						list.add(results.getString(DatabaseConstants.BREW_NAME_STRING));
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

	public static void renameRecipe(Player player, String name) {
		//Potion stuff
		String uuid = player.getUniqueId().toString();
		ItemStack item = player.getInventory().getItemInMainHand();
		String currentRecipe = item.getItemMeta().getDisplayName();

		String query = "UPDATE " + Brewery.getDatabase("recipes") + "recipes SET name=? WHERE inventor=? AND name=?";
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
			if(e1 instanceof SQLIntegrityConstraintViolationException) {
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

		String query = "UPDATE " + Brewery.getDatabase("recipes") + "recipes SET flavortext=? WHERE inventor=? AND name=?";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)){
			stmt.setString(1, ChatColor.GRAY + flavortext);
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
}