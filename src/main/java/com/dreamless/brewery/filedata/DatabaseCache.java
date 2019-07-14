package com.dreamless.brewery.filedata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import org.bukkit.Material;

import com.dreamless.brewery.Brewery;

public class DatabaseCache {
	private static HashSet<Material> acceptableIngredients = new HashSet<Material>();
	
	public static void updateAcceptableIngredients() {
		// Clear current set
		acceptableIngredients.clear();
		
		String query = "SELECT name FROM " + Brewery.getDatabase(null) + "ingredients";
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
	
	public static boolean containsIngredient(Material material) {
		return acceptableIngredients.contains(material);
	}
}
