package com.dreamless.brewery.listeners;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.data.DatabaseCommunication;
import com.dreamless.brewery.player.BPlayer;
import com.dreamless.brewery.player.Wakeup;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class CommandListener implements CommandExecutor {

	public Brewery p = Brewery.breweryDriver;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		String cmd = "help";
		if (args.length > 0) {
			cmd = args[0];
		}

		if (cmd.equalsIgnoreCase("help")) {

			cmdHelp(sender, args);

		} else if (cmd.equalsIgnoreCase("reload")) {

			if (sender.hasPermission("brewery.cmd.reload")) {
				p.reload(sender);
				p.msg(sender, Brewery.getText("CMD_Reload"));
			} else {
				p.msg(sender, Brewery.getText("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("wakeup")) {

			if (sender.hasPermission("brewery.cmd.wakeup")) {
				cmdWakeup(sender, args);
			} else {
				p.msg(sender, Brewery.getText("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("create")) {

			if (sender.hasPermission("brewery.cmd.create")) {
				cmdCreate(sender, args);
			} else {
				p.msg(sender, Brewery.getText("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("info")) {

			if (args.length > 1) {
				if (sender.hasPermission("brewery.cmd.infoOther")) {
					cmdInfo(sender, args[1]);
				} else {
					p.msg(sender, Brewery.getText("Error_NoPermissions"));
				}
			} else {
				if (sender.hasPermission("brewery.cmd.info")) {
					cmdInfo(sender, null);
				} else {
					p.msg(sender, Brewery.getText("Error_NoPermissions"));
				}
			} 
		} else if (cmd.equalsIgnoreCase("list") || cmd.equalsIgnoreCase("view") || cmd.equalsIgnoreCase("show")) {

			if (sender.hasPermission("brewery.cmd.claim")) {
				cmdList(sender, args);
			} else {
				p.msg(sender, Brewery.getText("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("purgeplayer")) {

			if (sender.hasPermission("brewery.cmd.purge")) {
				cmdPurgePlayer(sender, args);
			} else {
				p.msg(sender, Brewery.getText("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("rename") || cmd.equalsIgnoreCase("name")) {

			if (sender.hasPermission("brewery.cmd.claim")) {
				cmdRename(sender, args);
			} else {
				p.msg(sender, Brewery.getText("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("flavor") || cmd.equalsIgnoreCase("flavortext")) {

			if (sender.hasPermission("brewery.cmd.claim")) {
				cmdFlavorText(sender, args);
			} else {
				p.msg(sender, Brewery.getText("Error_NoPermissions"));
			}

		}else if (cmd.equalsIgnoreCase("drunkenness") || cmd.equalsIgnoreCase("toggle")) {
				cmdToggleDrunk(sender, args);
		} else {
			//p.getServer().getPlayerExact(cmd) != null
			UUID player = null;
			try {
				player = getUUID(cmd);
			} catch (Exception e) {
				p.msg(sender, "Hmm...parse failure?");
			}
			if (player != null  || BPlayer.hasPlayerbyName(cmd)) {

				if (args.length == 1) {
					if (sender.hasPermission("brewery.cmd.infoOther")) {
						cmdInfo(sender, cmd);
					}
				} else {
					if (sender.hasPermission("brewery.cmd.player")) {
						cmdPlayer(sender, args);
					} else {
						p.msg(sender, Brewery.getText("Error_NoPermissions"));
					}
				}

			} else {

				p.msg(sender, Brewery.getText("Error_UnknownCommand"));
				p.msg(sender, Brewery.getText("Error_ShowHelp"));

			}
		}
		return true;
	}

	public void cmdHelp(CommandSender sender, String[] args) {

		int page = 1;
		if (args.length > 1) {
			page = p.parseInt(args[1]);
		}

		ArrayList<String> commands = getCommands(sender);

		if (page == 1) {
			p.msg(sender, "&6" + p.getDescription().getName() + " v" + p.getDescription().getVersion());	
		}

		p.list(sender, commands, page);

	}

	public ArrayList<String> getCommands(CommandSender sender) {

		ArrayList<String> cmds = new ArrayList<String>();
		cmds.add(Brewery.getText("Help_Help"));

		if (sender.hasPermission("brewery.cmd.player")) {
			cmds.add (Brewery.getText("Help_Player"));
		}

		if (sender.hasPermission("brewery.cmd.info")) {
			cmds.add (Brewery.getText("Help_Info"));
		}

		if (sender.hasPermission("brewery.cmd.unlabel")) {
			cmds.add (Brewery.getText("Help_UnLabel"));
		}

		if (sender.hasPermission("brewery.cmd.copy")) {
			cmds.add (Brewery.getText("Help_Copy"));
		}

		if (sender.hasPermission("brewery.cmd.delete")) {
			cmds.add (Brewery.getText("Help_Delete"));
		}

		if (sender.hasPermission("brewery.cmd.infoOther")) {
			cmds.add (Brewery.getText("Help_InfoOther"));
		}

		if (sender.hasPermission("brewery.cmd.wakeup")) {
			cmds.add(Brewery.getText("Help_Wakeup"));
			cmds.add(Brewery.getText("Help_WakeupList"));
			cmds.add(Brewery.getText("Help_WakeupCheck"));
			cmds.add(Brewery.getText("Help_WakeupCheckSpecific"));
			cmds.add(Brewery.getText("Help_WakeupAdd"));
			cmds.add(Brewery.getText("Help_WakeupRemove"));
		}

		if (sender.hasPermission("brewery.cmd.reload")) {
			cmds.add(Brewery.getText("Help_Reload"));
		}

		if (sender.hasPermission("brewery.cmd.persist")) {
			cmds.add(Brewery.getText("Help_Persist"));
		}

		if (sender.hasPermission("brewery.cmd.static")) {
			cmds.add(Brewery.getText("Help_Static"));
		}

		if (sender.hasPermission("brewery.cmd.create")) {
			cmds.add(Brewery.getText("Help_Create"));
		}
		return cmds;
	}
	
	public void cmdList(CommandSender sender, String[] args) {
		Player player = null;
		if(sender instanceof Player) {
			player = (Player) sender;
		} else {
			return;
		}
		boolean claimed;
		if (args.length < 2) {
			claimed = true;
		} else {
			claimed = !(args[1].equalsIgnoreCase("unclaimed"));
		}
		p.msg(sender, DatabaseCommunication.listPlayerRecipes(player, claimed));
		
	}
		
	public void cmdPurgePlayer(CommandSender sender, String[] args) {
		if(args.length < 1) {
			return;
		}
		DatabaseCommunication.purgePlayer(args[1]);
		p.msg(sender, args[1] + "'s recipes have all been removed.");
	}
	
	public void cmdRename(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			//Check if given a name
			String newName = "";
			for(int i = 1; i < args.length; i++) {
				newName += args[i] + " ";
			}
			newName = newName.trim();
			if(newName.isEmpty()) {
				p.msg(sender, "You must give the brew a name!");
				return;
			}
			
			//Get Potion
			Player player = (Player) sender;
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null) {//Something in the hand
				NBTItem nbti = new NBTItem(hand);
				if(nbti.hasKey("brewery")) {
					if(nbti.hasKey("placedInBrewer")) {
						p.msg(sender, "You cannot name brews that are aging or distilling.");
					} else {
					DatabaseCommunication.renameRecipe(player, newName);
					}
				} else {
					p.msg(sender, Brewery.getText("Error_ItemNotBreweryPotion"));
				}
			} else {
				p.msg(sender, Brewery.getText("Error_ItemNotPotion"));
			}
		} else {
			p.msg(sender, Brewery.getText("Error_PlayerCommand"));
		}
	}
	
	public void cmdFlavorText(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			//Check if given a name
			String newFlavorText = "";
			for(int i = 1; i < args.length; i++) {
				newFlavorText += args[i] + " ";
			}
			newFlavorText = newFlavorText.trim();
			if(newFlavorText.isEmpty()) {
				p.msg(sender, "You must supply new flavor text!");
				return;
			}
			
			//Get Potion
			Player player = (Player) sender;
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null) {//Something in the hand
				NBTItem nbti = new NBTItem(hand);
				if(nbti.hasKey("brewery")) {
					if(nbti.hasKey("placedInBrewer")) {
						p.msg(sender, "You cannot give flavor text to brews that are aging or distilling.");
					} else {
					DatabaseCommunication.giveRecipeFlavorText(player, newFlavorText);
					}
				} else {
					p.msg(sender, Brewery.getText("Error_ItemNotBreweryPotion"));
				}
			} else {
				p.msg(sender, Brewery.getText("Error_ItemNotPotion"));
			}
		} else {
			p.msg(sender, Brewery.getText("Error_PlayerCommand"));
		}
	}
	
	public void cmdToggleDrunk(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			String toggle;
			if(args.length < 2) {
				toggle = "false";
			} else {
				toggle = args[1];
			}
			switch(toggle) {
			case "true":
			case "enable":
				BPlayer.toggleDrunkEffects(true, ((Player) sender).getUniqueId().toString());
				break;
			case "false":
			case "disable":
				BPlayer.toggleDrunkEffects(false, ((Player) sender).getUniqueId().toString());
				break;
			default:
				BPlayer.toggleDrunkEffects(false, ((Player) sender).getUniqueId().toString());
				break;
			}
		}
	}
	
	public void cmdWakeup(CommandSender sender, String[] args) {

		if (args.length == 1) {
			cmdHelp(sender, args);
			return;
		}

		if (args[1].equalsIgnoreCase("add")) {

			Wakeup.set(sender);

		} else if (args[1].equalsIgnoreCase("list")){

			int page = 1;
			String world = null;
			if (args.length > 2) {
				page = p.parseInt(args[2]);
			}
			if (args.length > 3) {
				world = args[3];
			}
			Wakeup.list(sender, page, world);

		} else if (args[1].equalsIgnoreCase("remove")){

			if (args.length > 2) {
				int id = p.parseInt(args[2]);
				Wakeup.remove(sender, id);
			} else {
				p.msg(sender, Brewery.getText("Etc_Usage"));
				p.msg(sender, Brewery.getText("Help_WakeupRemove"));
			}

		} else if (args[1].equalsIgnoreCase("check")){

			int id = -1;
			if (args.length > 2) {
				id = p.parseInt(args[2]);
				if (id < 0) {
					id = 0;
				}
			}
			Wakeup.check(sender, id, id == -1);

		} else if (args[1].equalsIgnoreCase("cancel")){

			Wakeup.cancel(sender);

		} else {

			p.msg(sender, Brewery.getText("Error_UnknownCommand"));
			p.msg(sender, Brewery.getText("Error_ShowHelp"));

		}
	}

	public void cmdPlayer(CommandSender sender, String[] args) {

		int drunkeness = p.parseInt(args[1]);
		if (drunkeness < 0) {
			return;
		}
		int quality = -1;
		if (args.length > 2) {
			quality = p.parseInt(args[2]);
			if (quality < 1 || quality > 10) {
				p.msg(sender, Brewery.getText("CMD_Player_Error"));
				return;
			}
		}

		String playerName = args[0];
		Player player = null;
		try {
			player = Bukkit.getPlayer(getUUID(playerName));
		} catch (Exception e) {
			p.msg(sender, "Error: UUID failure, " + playerName + " may not exist.");
		}
		BPlayer bPlayer;
		if (player == null) {
			bPlayer = BPlayer.getByName(playerName);
		} else {
			bPlayer = BPlayer.get(player);
		}
		if (bPlayer == null && player != null) {
			if (drunkeness == 0) {
				return;
			}
			bPlayer = BPlayer.addPlayer(player);
		}
		if (bPlayer == null) {
			return;
		}

		if (drunkeness == 0) {
			bPlayer.remove();
		} else {
			bPlayer.setData(drunkeness);
		}

		if (drunkeness > 100) {
			if (player != null) {
				bPlayer.drinkCap(player);
			} else {
				if (!BPlayer.overdrinkKick) {
					bPlayer.setData(100);
				}
			}
		}
		p.msg(sender, Brewery.getText("CMD_Player", playerName, "" + drunkeness	));

	}

	public void cmdInfo(CommandSender sender, String playerName) {

		if (playerName == null) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				playerName = player.getName();
			} else {
				p.msg(sender, Brewery.getText("Error_PlayerCommand"));
				return;
			}
		}
		
		Player player = null;
		try{ 
			player = Bukkit.getPlayer(getUUID(playerName));
		} catch (Exception e) {
			p.msg(sender, "Error: UUID failure, " + playerName + " may not exist.");
		}
		
		BPlayer bPlayer;
		if (player == null) {
			bPlayer = BPlayer.getByName(playerName);
		} else {
			bPlayer = BPlayer.get(player);
		}
		if (bPlayer == null) {
			p.msg(sender, Brewery.getText("CMD_Info_NotDrunk", playerName));
		} else {
			p.msg(sender, Brewery.getText("CMD_Info_Drunk", playerName, "" + bPlayer.getDrunkeness()));
		}

	}

	public void cmdCreate(CommandSender sender, String[] args) {

		if (args.length < 2) {
			p.msg(sender, Brewery.getText("Etc_Usage"));
			p.msg(sender, Brewery.getText("Help_Create"));
			return;
		}

		int quality = 10;
		boolean hasQuality = false;
		String pName = null;
		if (args.length > 2) {
			quality = p.parseInt(args[args.length - 1]);

			if (quality <= 0 || quality > 10) {
				pName = args[args.length - 1];
				if (args.length > 3) {
					quality = p.parseInt(args[args.length - 2]);
				}
			}
			if (quality > 0 && quality <= 10) {
				hasQuality = true;
			} else {
				quality = 10;
			}
		}
		Player player = null;
		if (pName != null) {
			try {
				player = Bukkit.getPlayer(getUUID(pName));
			} catch (Exception e) {
				p.msg(sender, "Error: UUID failure, " + pName + " may not exist.");
			}
		}
		
		if (sender instanceof Player || player != null) {
			if (player == null) {
				player = ((Player) sender);
			}
			int stringLength = args.length - 1;
			if (pName != null) {
				stringLength--;
			}
			if (hasQuality) {
				stringLength--;
			}

			String name;
			if (stringLength > 1) {
				StringBuilder builder = new StringBuilder(args[1]);

				for (int i = 2; i < stringLength + 1; i++) {
					builder.append(" ").append(args[i]);
				}
				name = builder.toString();
			} else {
				name = args[1];
			}

			if (player.getInventory().firstEmpty() == -1) {
				p.msg(sender, Brewery.getText("CMD_Copy_Error", "1"));
				return;
			}

			p.msg(sender, Brewery.getText("Error_NoBrewName", name));

		} else {
			p.msg(sender, Brewery.getText("Error_PlayerCommand"));
		}
	}
	
	private UUID getUUID(String name) throws ParseException, org.json.simple.parser.ParseException {
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

}