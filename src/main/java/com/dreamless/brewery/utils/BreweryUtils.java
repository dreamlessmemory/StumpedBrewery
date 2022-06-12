package com.dreamless.brewery.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.ChatPaginator;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.dreamless.brewery.entity.BreweryCauldron;

public final class BreweryUtils {
    
	private static final int WRAP_SIZE = 30;
	
    public static String toBase64(Inventory inventory) {
    	if(inventory == null)
        	return "";
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());
            
            // Save every element in the list
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }
            
            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }        
    }
    
    public static Inventory fromBase64(String data, BreweryCauldron cauldron) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(cauldron, dataInput.readInt(), "Brewery Cauldron");
    
            // Read the serialized inventory
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }
            dataInput.close();
            return inventory;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
    
    public static BlockFace getPlayerDirection(Player player)
    {
 
        BlockFace dir = null;
     
        float y = player.getLocation().getYaw();
     
        if( y < 0 ){y += 360;}
     
        y %= 360;
     
        int i = (int)((y+8) / 22.5);
     
        if(i == 0){dir = BlockFace.WEST;}
        else if(i == 1){dir = BlockFace.WEST_NORTH_WEST;}
        else if(i == 2){dir = BlockFace.NORTH_WEST;}
        else if(i == 3){dir = BlockFace.NORTH_NORTH_WEST;}
        else if(i == 4){dir = BlockFace.NORTH;}
        else if(i == 5){dir = BlockFace.NORTH_NORTH_EAST;}
        else if(i == 6){dir = BlockFace.NORTH_EAST;}
        else if(i == 7){dir = BlockFace.EAST_NORTH_EAST;}
        else if(i == 8){dir = BlockFace.EAST;}
        else if(i == 9){dir = BlockFace.EAST_SOUTH_EAST;}
        else if(i == 10){dir = BlockFace.SOUTH_EAST;}
        else if(i == 11){dir = BlockFace.SOUTH_SOUTH_EAST;}
        else if(i == 12){dir = BlockFace.SOUTH;}
        else if(i == 13){dir = BlockFace.SOUTH_SOUTH_WEST;}
        else if(i == 14){dir = BlockFace.SOUTH_WEST;}
        else if(i == 15){dir = BlockFace.WEST_SOUTH_WEST;}
        else {dir = BlockFace.WEST;}
     
        return dir;
 
    }
    
    public static String getItemName(ItemStack item) {
    	String words[]=item.getType().toString().split("_");  
        String capitalizeWord="";  
        for(String w:words){  
            String first=w.substring(0,1);  
            String afterfirst=w.substring(1);  
            capitalizeWord+=first+afterfirst.toLowerCase()+" ";  
        }  
        return capitalizeWord.trim(); 
	}
    
    public static UUID getUUID(String name) throws ParseException, org.json.simple.parser.ParseException {
        String url = "https://api.mojang.com/users/profiles/minecraft/"+name;
        try {
            String UUIDJson = IOUtils.toString(new URL(url), "US-ASCII");
            if(UUIDJson.isEmpty()) {
            	return null;
            }
            JSONObject UUIDObject = (JSONObject) JSONValue.parseWithException(UUIDJson);       
            String tempID = UUIDObject.get("id").toString();
            tempID = tempID.substring(0,  8) + "-" + tempID.substring(8,  12) + "-" + tempID.substring(12,  16) + "-" + tempID.substring(16,  20) + "-" + tempID.substring(20);
            return UUID.fromString(tempID);
        } catch (IOException e) {
            e.printStackTrace();
        }       
        return null;
    }
    
    public static List<String> wordWrap(String input){
    	return Arrays.asList(ChatPaginator.wordWrap(ChatColor.GRAY + input, WRAP_SIZE));
    }
    
	public static boolean usesBucket(ItemStack item) {
		switch (item.getType()) {
		case LAVA_BUCKET:
		case MILK_BUCKET:
		case WATER_BUCKET:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isAxe(ItemStack item) {
		switch (item.getType()) {
		case WOODEN_AXE:
		case GOLDEN_AXE:
		case STONE_AXE:
		case IRON_AXE:
		case DIAMOND_AXE:
		case NETHERITE_AXE:
			return true;
		default:
			return false;
		}
	}
}