package com.dreamless.brewery.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.recipe.RecipeEnum.Flavor;
import com.dreamless.brewery.recipe.RecipeEnum.IngredientType;

public class DatabaseCache {
	private static HashMap<Material, IngredientInformation> ingredientsMap = new HashMap<>();
	private static HashMap<BrewTypeRequirement, BrewType> brewTypesMap = new HashMap<>();

	public static void updateAcceptableIngredients() {
		// Clear current set
		ingredientsMap.clear();

		String query = "SELECT * FROM " + Brewery.getDatabase("ingredients") + "ingredients";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			ResultSet results;
			results = stmt.executeQuery();
			while (results.next()) {
				ingredientsMap.put(Material.getMaterial(results.getString(0)), new IngredientInformation(
						IngredientType.APPLE, IngredientType.APPLE, Flavor.SWEET, Flavor.SWEET));
				Brewery.breweryDriver
						.debugLog("updateAcceptableIngredients() - Added ingredient: " + results.getString(0));
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
			while (results.next()) {

			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	public static boolean containsIngredient(Material material) {
		return ingredientsMap.containsKey(material);
	}

	public static BrewType getBrewType(IngredientType primary, IngredientType secondary, byte grade) {
		return brewTypesMap.get(new BrewTypeRequirement(primary, secondary, grade));
	}
}
