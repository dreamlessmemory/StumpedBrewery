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

    public BreweryRecipe(String name, String inventorUUID, String flavorText) {
		this.name = name;
		inventorText = "Crafted by " + (inventorUUID.isEmpty() ? "an unknown brewer" : getInventorName(inventorUUID));
		this.flavorText.addAll(Arrays.asList(ChatPaginator.wordWrap(flavorText, WRAP_SIZE)));
	}
    
    public BreweryRecipe(String inventorUUID) {
    	this.name = BreweryRecipe.generateNewRecipeName(inventorUUID); 
		inventorText = "Crafted by " + (inventorUUID.isEmpty() ? "an unknown brewer" : getInventorName(inventorUUID));
		flavorText.addAll(Arrays.asList(ChatPaginator.wordWrap(ChatColor.GRAY +  Brewery.getText("Recipe_New_Flavortext"), WRAP_SIZE)));	
    }
	
	private static String generateNewRecipeName(String inventorUUID) {
		if(inventorUUID != null) {
			return getInventorName(inventorUUID) + "'s Novel Brew #" + DatabaseCommunication.getRecipeCount();
		}
		else
		{
			return "Novel Brew #" + DatabaseCommunication.getRecipeCount();
		}
	}

	public String getName() {
		return name;
	}
	
	private static String getInventorName(String inventorUUID) {
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
	
	public String getFlavorTextString() {
		String finalString = "";
		for(String string : flavorText) {
			finalString = finalString.concat(string);
		}
		return finalString;
	}
	
}
