package com.dreamless.brewery;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.graph.ElementOrder.Type;
import com.google.gson.reflect.TypeToken;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;

public class BCauldron {
	public static CopyOnWriteArrayList<BCauldron> bcauldrons = new CopyOnWriteArrayList<BCauldron>();

	private BIngredients ingredients = new BIngredients();
	private Block block;
	private int state = 1;
	private boolean cooking = false;

	public BCauldron(Block block, ItemStack ingredient) {
		this.block = block;
		add(ingredient);
		cooking = false;
		bcauldrons.add(this);
	}

	// loading from file
	public BCauldron(Block block, BIngredients ingredients, int state) {
		this.block = block;
		this.state = state;
		this.ingredients = ingredients;
		cooking = false;
		bcauldrons.add(this);
	}
	
	public BCauldron(Block block, BIngredients ingredients, int state, boolean cooking) {
		this.block = block;
		this.state = state;
		this.ingredients = ingredients;
		this.cooking = cooking;
		bcauldrons.add(this);
	}

	public void onUpdate() {//UPDATE THE POTION
		// Check if fire still alive
		if ((!block.getChunk().isLoaded() || fireActive()) && cooking) {
			// add a minute to cooking time
			state++;
			//Sound and particle effects
			block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().getX() + 0.5, block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
			block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 2.0f, 1.0f);
			//Run aspect calculation
			ingredients.fermentOneStep(state);
		} else { //no fire, stop cooking
			if(cooking = true) {
				cooking = false;
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 2.0f, 1.0f);
			}
		}
	}

	// add an ingredient to the cauldron
	public void add(ItemStack ingredient){
		ingredient = new ItemStack(ingredient.getType(), 1, ingredient.getDurability());
		ingredients.add(ingredient);
		block.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
		if (state > 1) {
			state--;
		}
	}

	// get cauldron by Block
	public static BCauldron get(Block block) {
		for (BCauldron bcauldron : bcauldrons) {
			if (bcauldron.block.equals(block)) {
				return bcauldron;
			}
		}
		return null;
	}

	// get cauldron from block and add given ingredient
	public static boolean ingredientAdd(Block block, ItemStack ingredient) {
		// if not empty
		if (getFillLevel(block) != 0) {
			BCauldron bcauldron = get(block);
			if (bcauldron != null) {
				bcauldron.add(ingredient);
				return true;
			} else {
				new BCauldron(block, ingredient);
				//TODO: check if in purge list, then remove from purge list?
				return true;
			}
		}
		return false;
	}

	// fills players bottle with cooked brew
	public static boolean fill(Player player, Block block) {
		BCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (!player.hasPermission("brewery.cauldron.fill")) {
				Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Perms_NoCauldronFill"));
				return true;
			}
			ItemStack potion = bcauldron.ingredients.cook(bcauldron.state, player);
			
			if (potion != null) {
				
				Levelled cauldronData = (Levelled) block.getBlockData();
				int level = cauldronData.getLevel();
				
				if(level > cauldronData.getMaximumLevel()) {
					level = cauldronData.getMaximumLevel();
				} else if (level <= 0) {
					bcauldrons.remove(bcauldron);
					return false;
				}
				
				cauldronData.setLevel(--level);
				block.setBlockData(cauldronData);
				block.getState().update();
				
				if (level == 0) {
					bcauldrons.remove(bcauldron);
				}
				giveItem(player, potion);
				return true;
			}
		}
		return false;
	}

	// 0 = empty, 1 = something in, 2 = full
	public static int getFillLevel(Block block) {
		if (block.getType() == Material.CAULDRON) {
			Levelled fillLevel = (Levelled) block.getState().getBlockData();
			return fillLevel.getLevel();
		}
		return 0;
	}

	// prints the current cooking time to the player
	public static void printTime(Player player, Block block) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Error_NoPermissions"));
			return;
		}
		BCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (bcauldron.state > 1) {
				Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Player_CauldronInfo1", "" + bcauldron.state));
			} else {
				Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Player_CauldronInfo2"));
			}
		}
	}
	
	public static void printContents(Player player, Block block) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Error_NoPermissions"));
			return;
		}
		BCauldron bcauldron = get(block);
		if (bcauldron != null) {
			Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Player_CauldronContents", "" + bcauldron.ingredients.getContents()));
		}
	}
	
	// reset to normal cauldron
	public static void remove(Block block) {
		if (getFillLevel(block) != 0) {
			BCauldron bcauldron = get(block);
			if (bcauldron != null) {
				bcauldrons.remove(bcauldron);
				//TODO add cauldron to purge list
			}
		}
	}

	// unloads cauldrons that are in a unloading world
	// as they were written to file just before, this is safe to do
	public static void onUnload(String name) {
		for (BCauldron bcauldron : bcauldrons) {
			if (bcauldron.block.getWorld().getName().equals(name)) {
				bcauldrons.remove(bcauldron);
			}
		}
	}

	public static void save(ConfigurationSection config, ConfigurationSection oldData) {
		Brewery.breweryDriver.createWorldSections(config);

		int id = 0;
		if (!bcauldrons.isEmpty()) {
			
			for (BCauldron cauldron : bcauldrons) {
				String worldName = cauldron.block.getWorld().getName();
				String prefix;

				if (worldName.startsWith("DXL_")) {
					prefix = Brewery.breweryDriver.getDxlName(worldName) + "." + id;
				} else {
					prefix = cauldron.block.getWorld().getUID().toString() + "." + id;
				}

				config.set(prefix + ".block", cauldron.block.getX() + "/" + cauldron.block.getY() + "/" + cauldron.block.getZ());
				if (cauldron.state != 1) {
					config.set(prefix + ".state", cauldron.state);
				}
				config.set(prefix + ".ingredients", cauldron.ingredients.serializeIngredients());
				config.set(prefix + ".cooking", cauldron.cooking);
				
				
				//SQL
				//Columns are worldname, location, ingredients, aspects, cooking
				
				//Location
				String location = Brewery.gson.toJson(cauldron.block.getLocation().serialize());
				Brewery.breweryDriver.debugLog(location);
				
				//Ingredients
				String ingredients = Brewery.gson.toJson(cauldron.ingredients.serializeIngredients());
				Brewery.breweryDriver.debugLog(ingredients);
				
				//Aspects
				String aspects = Brewery.gson.toJson(cauldron.ingredients.getAspects());
				Brewery.breweryDriver.debugLog(aspects);
				
				String query = "REPLACE cauldrons SET idcauldrons=?, location=?, ingredients=?, aspects=?, state=?, cooking=?";
				try(PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
					stmt.setInt(1, id);
					stmt.setString(2, location);
					stmt.setString(3, ingredients);
					stmt.setString(4, aspects);
					stmt.setInt(5, cauldron.state);
					stmt.setBoolean(6, cauldron.cooking);
					
					Brewery.breweryDriver.debugLog(stmt.toString());
					
					stmt.executeUpdate();
				} catch (SQLException e1) {
					e1.printStackTrace();
					return;
				}				
				id++;
			}
			
			

			
			
		}
		//clean up extras
		String query = "DELETE FROM cauldrons WHERE idcauldrons >=?";
		try(PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setInt(1, id);
			Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}
	}

	// bukkit bug not updating the inventory while executing event, have to
	// schedule the give
	public static void giveItem(final Player player, final ItemStack item) {
		Brewery.breweryDriver.getServer().getScheduler().runTaskLater(Brewery.breweryDriver, new Runnable() {
			public void run() {
				player.getInventory().addItem(item);
			}
		}, 1L);
	}
	
	private boolean fireActive() {
		return block.getRelative(BlockFace.DOWN).getType() == Material.FIRE || block.getRelative(BlockFace.DOWN).getType() == Material.MAGMA_BLOCK
				|| block.getRelative(BlockFace.DOWN).getType() == Material.LAVA;
	}
	
	public static boolean isCooking(Block block) {
		BCauldron bcauldron = get(block);
		if(bcauldron != null) {
			return bcauldron.cooking;
		} else {
			return false;
		}
	}

	public static void setCooking(Block block, boolean cooking) {
		BCauldron bcauldron = get(block);
		if(bcauldron!= null) {
			bcauldron.cooking = cooking;
			if(cooking) {
				bcauldron.ingredients.startCooking();
			}
		}
	}
	
	public void startCooking() {
		
	}
}