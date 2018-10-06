package com.dreamless.brewery.listeners;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.dreamless.brewery.BIngredients;
import com.dreamless.brewery.BPlayer;
import com.dreamless.brewery.BRecipe;
import com.dreamless.brewery.Brew;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.Wakeup;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
				p.msg(sender, p.languageReader.get("CMD_Reload"));
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("wakeup")) {

			if (sender.hasPermission("brewery.cmd.wakeup")) {
				cmdWakeup(sender, args);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("create")) {

			if (sender.hasPermission("brewery.cmd.create")) {
				cmdCreate(sender, args);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("info")) {

			if (args.length > 1) {
				if (sender.hasPermission("brewery.cmd.infoOther")) {
					cmdInfo(sender, args[1]);
				} else {
					p.msg(sender, p.languageReader.get("Error_NoPermissions"));
				}
			} else {
				if (sender.hasPermission("brewery.cmd.info")) {
					cmdInfo(sender, null);
				} else {
					p.msg(sender, p.languageReader.get("Error_NoPermissions"));
				}
			}

		} else if (cmd.equalsIgnoreCase("copy") || cmd.equalsIgnoreCase("cp")) {

			if (sender.hasPermission("brewery.cmd.copy")) {
				if (args.length > 1) {
					cmdCopy(sender, p.parseInt(args[1]));
				} else {
					cmdCopy(sender, 1);
				}
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("delete") || cmd.equalsIgnoreCase("rm") || cmd.equalsIgnoreCase("remove")) {

			if (sender.hasPermission("brewery.cmd.delete")) {
				cmdDelete(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("persist") || cmd.equalsIgnoreCase("persistent")) {

			if (sender.hasPermission("brewery.cmd.persist")) {
				cmdPersist(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("unlabel")) {

			if (sender.hasPermission("brewery.cmd.unlabel")) {
				cmdUnlabel(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("list")) {

			if (sender.hasPermission("brewery.cmd.claim")) {
				cmdList(sender, args);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("purge")) {

			if (sender.hasPermission("brewery.cmd.purge")) {
				cmdPurge(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("claim")) {

			if (sender.hasPermission("brewery.cmd.claim")) {
				cmdClaim(sender, args);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

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
						p.msg(sender, p.languageReader.get("Error_NoPermissions"));
					}
				}

			} else {

				p.msg(sender, p.languageReader.get("Error_UnknownCommand"));
				p.msg(sender, p.languageReader.get("Error_ShowHelp"));

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
		cmds.add(p.languageReader.get("Help_Help"));

		if (sender.hasPermission("brewery.cmd.player")) {
			cmds.add (p.languageReader.get("Help_Player"));
		}

		if (sender.hasPermission("brewery.cmd.info")) {
			cmds.add (p.languageReader.get("Help_Info"));
		}

		if (sender.hasPermission("brewery.cmd.unlabel")) {
			cmds.add (p.languageReader.get("Help_UnLabel"));
		}

		if (sender.hasPermission("brewery.cmd.copy")) {
			cmds.add (p.languageReader.get("Help_Copy"));
		}

		if (sender.hasPermission("brewery.cmd.delete")) {
			cmds.add (p.languageReader.get("Help_Delete"));
		}

		if (sender.hasPermission("brewery.cmd.infoOther")) {
			cmds.add (p.languageReader.get("Help_InfoOther"));
		}

		if (sender.hasPermission("brewery.cmd.wakeup")) {
			cmds.add(p.languageReader.get("Help_Wakeup"));
			cmds.add(p.languageReader.get("Help_WakeupList"));
			cmds.add(p.languageReader.get("Help_WakeupCheck"));
			cmds.add(p.languageReader.get("Help_WakeupCheckSpecific"));
			cmds.add(p.languageReader.get("Help_WakeupAdd"));
			cmds.add(p.languageReader.get("Help_WakeupRemove"));
		}

		if (sender.hasPermission("brewery.cmd.reload")) {
			cmds.add(p.languageReader.get("Help_Reload"));
		}

		if (sender.hasPermission("brewery.cmd.persist")) {
			cmds.add(p.languageReader.get("Help_Persist"));
		}

		if (sender.hasPermission("brewery.cmd.static")) {
			cmds.add(p.languageReader.get("Help_Static"));
		}

		if (sender.hasPermission("brewery.cmd.create")) {
			cmds.add(p.languageReader.get("Help_Create"));
		}
		//TODO claim
		//TODO rename
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
			//Brewery.breweryDriver.debugLog(args[2]);
		}
		p.msg(sender, BRecipe.listPlayerRecipes(player, claimed));
		
	}
	
	public void cmdPurge(CommandSender sender) {
		if(BRecipe.purgeRecipes()) {
			p.msg(sender, "Recipes purged");
		} else {
			p.msg(sender, "Recipe purge failed");
		}
		
	}
	
	
	//TODO: claim brew
	
	public void cmdClaim (CommandSender sender, String[] args) {
		//Get Player
		Player player = null;
		if(sender instanceof Player) {
			player = (Player) sender;
		} else {
			return;
		}
		int claimNumber = 0;
		//Parse 
		try {
			claimNumber = Integer.parseInt(args[1]);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			Brewery.breweryDriver.msg(sender, "You need to specify a number");
			return;
		}
		
		//Parse the name
		String newName = "";
		for(int i = 2; i < args.length; i++) {
			newName += args[i] + " ";
		}
		newName = newName.trim();
		if(newName.contains("#")) {
			p.msg(sender, "You cannot use # in a name!");
			return;
		}
		
		if(BRecipe.claimRecipe(player, claimNumber, newName)) {
			p.msg(sender, newName + " has been claimed! Congratulations!");
		} else {
			p.msg(sender, "Failed to claim!");
		}
	}
	//TODO: rename brew
	
	
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
				p.msg(sender, p.languageReader.get("Etc_Usage"));
				p.msg(sender, p.languageReader.get("Help_WakeupRemove"));
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

			p.msg(sender, p.languageReader.get("Error_UnknownCommand"));
			p.msg(sender, p.languageReader.get("Error_ShowHelp"));

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
				p.msg(sender, p.languageReader.get("CMD_Player_Error"));
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
			bPlayer.setData(drunkeness, quality);
		}

		if (drunkeness > 100) {
			if (player != null) {
				bPlayer.drinkCap(player);
			} else {
				if (!BPlayer.overdrinkKick) {
					bPlayer.setData(100, 0);
				}
			}
		}
		p.msg(sender, p.languageReader.get("CMD_Player", playerName, "" + drunkeness, "" + bPlayer.getQuality()));

	}

	public void cmdInfo(CommandSender sender, String playerName) {

		if (playerName == null) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				playerName = player.getName();
			} else {
				p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
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
			p.msg(sender, p.languageReader.get("CMD_Info_NotDrunk", playerName));
		} else {
			p.msg(sender, p.languageReader.get("CMD_Info_Drunk", playerName, "" + bPlayer.getDrunkeness(), "" + bPlayer.getQuality()));
		}

	}

	public void cmdCopy(CommandSender sender, int count) {

		if (sender instanceof Player) {
			if (count < 1 || count > 36) {
				p.msg(sender, p.languageReader.get("Etc_Usage"));
				p.msg(sender, p.languageReader.get("Help_Copy"));
				return;
			}
			Player player = (Player) sender;
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					while (count > 0) {
						ItemStack item = brew.copy(hand);
						if (!(player.getInventory().addItem(item)).isEmpty()) {
							p.msg(sender, p.languageReader.get("CMD_Copy_Error", "" + count));
							return;
						}
						count--;
					}
					if (brew.isPersistent()) {
						p.msg(sender, p.languageReader.get("CMD_CopyNotPersistent"));
					}
					return;
				}
			}

			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));

		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	public void cmdDelete(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					if (brew.isPersistent()) {
						p.msg(sender, p.languageReader.get("CMD_PersistRemove"));
					} else {
						brew.remove(hand);
						player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					}
					return;
				}
			}
			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	public void cmdPersist(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					if (brew.isPersistent()) {
						brew.removePersistence();
						p.msg(sender, p.languageReader.get("CMD_UnPersist"));
					} else {
						brew.makePersistent();
						p.msg(sender, p.languageReader.get("CMD_Persistent"));
					}
					brew.touch();
					return;
				}
			}
			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	public void cmdUnlabel(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					brew.unLabel(hand);
					brew.touch();
					p.msg(sender, p.languageReader.get("CMD_UnLabel"));
					return;
				}
			}
			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	public void cmdCreate(CommandSender sender, String[] args) {

		if (args.length < 2) {
			p.msg(sender, p.languageReader.get("Etc_Usage"));
			p.msg(sender, p.languageReader.get("Help_Create"));
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
				p.msg(sender, p.languageReader.get("CMD_Copy_Error", "1"));
				return;
			}

			BRecipe recipe = null;
			if (recipe != null) {
				player.getInventory().addItem(recipe.create(quality));
			} else {
				p.msg(sender, p.languageReader.get("Error_NoBrewName", name));
			}

		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
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