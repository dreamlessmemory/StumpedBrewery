package com.dreamless.brewery.brew;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.data.DatabaseCommunication;

public class BreweryRecipe {

private static final int WRAP_SIZE = 30;
	
	private final String name;
	private final String inventorText;
    private ArrayList<String> flavorText = new ArrayList<String>();
    
    public BreweryRecipe() {
		this.name = "An Unknown Brew";
		inventorText = "Not invented...";
		this.flavorText.add("A strange brew with no effects...");
	}
    
    public BreweryRecipe(String name, String inventorUUID, String flavorText) {
		this.name = name;
		inventorText = "First invented by " + getInventorName(inventorUUID);
		this.flavorText.add(flavorText);
	}
    
    public BreweryRecipe(String inventorUUID) {
    	this.name = BreweryRecipe.generateNewRecipeName(); 
		inventorText = "First invented by " + getInventorName(inventorUUID);
		flavorText.addAll(Arrays.asList(ChatPaginator.wordWrap(ChatColor.GRAY +  Brewery.getText("Recipe_New_Flavortext"), WRAP_SIZE)));	
    }
	
	private static String generateNewRecipeName() {
		return "Novel Brew #" + DatabaseCommunication.getRecipeCount();
	}

	public String getName() {
		return name;
	}
	
	private String getInventorName(String inventorUUID) {
		String inventorName = "an unknown brewer";
		if(inventorUUID != null) {
			Player inventor = Bukkit.getOfflinePlayer(UUID.fromString(inventorUUID)).getPlayer();
			if(inventor != null) {
				inventorName = inventor.getDisplayName();
			}
		}
		return inventorName;
	}
	
	public ArrayList<String> getFlavorText() {	
		ArrayList<String> flavor = new ArrayList<String>();
		flavor.add(inventorText);
		flavor.addAll(flavorText);
		return flavor;
		
	}
	
}
