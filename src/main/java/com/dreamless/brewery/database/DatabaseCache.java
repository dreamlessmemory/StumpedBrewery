package com.dreamless.brewery.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.recipe.RecipeEnum.IngredientType;
import com.sun.org.apache.bcel.internal.generic.NEW;

public class DatabaseCache {
	private static HashSet<Material> acceptableIngredients = new HashSet<>();
	private static HashMap<BrewTypeRequirement, BrewType> brewTypesMap = new HashMap<>();
	
	public static void updateAcceptableIngredients() {
		// Clear current set
		acceptableIngredients.clear();
		
		String query = "SELECT name FROM " + Brewery.getDatabase("ingredients") + "ingredients";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			ResultSet results;
			results = stmt.executeQuery();
			while(results.next()) {
				acceptableIngredients.add(Material.getMaterial(results.getString(0)));
				Brewery.breweryDriver.debugLog("updateAcceptableIngredients() - Added ingredient: " + results.getString(0));
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void updateBrewTypesMap() {
		brewTypesMap.clear();
		
		String query = "SELECT * FROM " + Brewery.getDatabase("brewtypes") + "brewtypes";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			ResultSet results;
			results = stmt.executeQuery();
			while(results.next()) {
								
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	public static boolean containsIngredient(Material material) {
		return acceptableIngredients.contains(material);
	}
	
	public static BrewType getBrewType(IngredientType primary, IngredientType secondary, byte grade) {
		return brewTypesMap.get(new BrewTypeRequirement(primary, secondary, grade));
	}
}
