package com.dreamless.brewery;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.Bisected;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;
import org.apache.commons.lang.ArrayUtils;

public class Barrel implements InventoryHolder {

	public static CopyOnWriteArrayList<Barrel> barrels = new CopyOnWriteArrayList<Barrel>();
	private static int check = 0;
	
	//Difficulty adjustments
	public static double minutesPerYear = 20.0; 
	
	private Block spigot;
	private int[] woodsloc = null; // location of wood Blocks
	private int[] stairsloc = null; // location of stair Blocks
	private byte signoffset;
	private boolean checked = false;
	private Inventory inventory;
	private float time;

	public Barrel(Block spigot, byte signoffset) {
		this.spigot = spigot;
		this.signoffset = signoffset;
	}
	
	public Barrel(Block spigot, byte sign, int[] woodsloc, int[] stairsloc, String inventory, float time) {
		this.spigot = spigot;
		this.signoffset = sign;
		this.time = time;
		this.woodsloc = woodsloc;
		this.stairsloc = stairsloc;
		try {
			this.inventory = BreweryUtils.fromBase64(inventory, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (woodsloc == null && stairsloc == null) {
			Block broken = getBrokenBlock(true);
			if (broken != null) {
				remove(broken, null);
				return;
			}
		}
		
		barrels.add(this);
	}


	public static void onUpdate() {
		//Brewery.breweryDriver.debugLog("Update Barrel");
		for (Barrel barrel : barrels) {
			// Minecraft day is 20 min, so add 1/20 to the time every minute
			//barrel.time += (1.0 / 20.0);
			barrel.time += (1.0 / minutesPerYear);
			//Brewery.breweryDriver.debugLog("Age Start?");
			barrel.ageContents((1.0 / minutesPerYear));
			//Brewery.breweryDriver.debugLog("Age end?");
		}
		if (check == 0 && barrels.size() > 0) {
			Barrel random = barrels.get((int) Math.floor(Math.random() * barrels.size()));
			if (random != null) {
				// You have been selected for a random search
				// We want to check at least one barrel every time
				random.checked = false;
			}
			new BarrelCheck().runTaskTimer(Brewery.breweryDriver, 1, 1);
		}
	}
	
	private void ageContents(double time) {
		if(inventory == null) {
			return;
		}
		ItemStack[] contents = inventory.getContents(); 
		for(int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			//Brewery.breweryDriver.debugLog("Pull item");
			if(item == null) {
				continue;
			}
			NBTItem nbti = new NBTItem(item);
			
			if(!nbti.hasKey("brewery")) {
				continue;//Not brewery, skip
			}
			//Brewery.breweryDriver.debugLog("Get item?");
			NBTCompound brewery = nbti.getCompound("brewery");
			
			NBTCompound aging = brewery.hasKey("aging") ? brewery.getCompound("aging") : brewery.addCompound("aging");
			double age = aging.hasKey("age") ? aging.getDouble("age") : 0; 
			double newAge = age + time;
			
			//Assign age now
			aging.setDouble("age", newAge);
			item = nbti.getItem();
			Brewery.breweryDriver.debugLog("Age is " + newAge);
			//Check new Age
			if(Math.floor(newAge) > Math.floor(age)) {//Logic - round down, then compare. Precondition, time =<1.0
				item = ageOneYear(item, getWood());
			}

			//Update Inventory
			inventory.setItem(i, item);
		}
	}
	
	public ItemStack ageOneYear(ItemStack item, byte woodType) {
	
		Brewery.breweryDriver.debugLog("AGING 1 YEAR : " + item.toString());
		
		//Pull NBT
		NBTItem nbti = new NBTItem(item);
		NBTCompound brewery = nbti.getCompound("brewery");
		NBTCompound aging = brewery.getCompound("aging");
		
		//Pull aspects
		NBTCompound aspectList = brewery.getCompound("aspects");
		Set<String> aspects = aspectList.getKeys();
		
		//Calculate new aspects
		int age = (int) Math.floor(aging.getDouble("age"));
		for(String currentAspect : aspects) {
			double aspectPotency = aspectList.getDouble(currentAspect);
			double agingBonus = Aspect.getStepBonus(age, currentAspect, "aging");
			double typeBonus = agingBonus * (Aspect.getStepBonus(age, brewery.getString("type"), "aging")/100);
			double newPotency = aspectPotency + agingBonus + typeBonus;
			if(newPotency <= 0) {
				newPotency = 0;
			}
			Brewery.breweryDriver.debugLog("Update Potency of " + currentAspect + ": " + aspectPotency + " + " + agingBonus + " + " + typeBonus + " -> " + newPotency);
			//Update NBT
			aspectList.setDouble(currentAspect, newPotency);
		}
		
		item = nbti.getItem();
		
		//Mask as Aging Brew
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		potionMeta.setDisplayName("#Aging Brew");
		ArrayList<String> agedFlavorText = new ArrayList<String>();
		if(!aging.hasKey("isAging") && aging.getBoolean("isAging") != true) {
			aging.setBoolean("isAging", true);
			item = nbti.getItem();

			agedFlavorText.add("An aging " +  brewery.getString("type").toLowerCase() + " brew.");
			agedFlavorText.add("This brew has aged for " + age + (age > 1 ? " years" : " year"));
		} else {
			//Update flavor text
			List<String> flavorText = potionMeta.getLore();
			agedFlavorText.add(flavorText.get(0));
			agedFlavorText.add("This brew has aged for " + age + " years");
		}
		
		potionMeta.setLore(agedFlavorText);
		potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(potionMeta);
		
		return item;
		
	}
	
	public boolean hasPermsOpen(Player player, PlayerInteractEvent event) {
		if (isLarge()) {
			if (!player.hasPermission("brewery.openbarrel.big")) {
				Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Error_NoBarrelAccess"));
				return false;
			}
		} else {
			if (!player.hasPermission("brewery.openbarrel.small")) {
				Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Error_NoBarrelAccess"));
				return false;
			}
		}
		return true;
	}

	// player opens the barrel
	public void open(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
		if (inventory == null) {
			if (isLarge()) {
				inventory = org.bukkit.Bukkit.createInventory(this, 27, Brewery.breweryDriver.languageReader.get("Etc_Barrel"));
			} else {
				inventory = org.bukkit.Bukkit.createInventory(this, 9, Brewery.breweryDriver.languageReader.get("Etc_Barrel"));
			}
		} else {
			if (time > 0) {
				// if nobody has the inventory opened
				if (inventory.getViewers().isEmpty()) {
					// if inventory contains potions
					if (inventory.contains(Material.POTION)) {
					}
				}
			}
		}
		// reset barreltime, potions have new age
		time = 0;
		player.openInventory(inventory);
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}

	// Returns true if this Block is part of this Barrel
	public boolean hasBlock(Block block) {
		if (block != null) {
			if (isWood(block.getType())) {
				if (hasWoodBlock(block)) {
					return true;
				}
			} else if (isStairs(block.getType())) {
				if (hasStairsBlock(block)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasWoodBlock(Block block) {
		if (woodsloc != null) {
			if (spigot.getWorld() != null && spigot.getWorld().equals(block.getWorld())) {
				if (woodsloc.length > 2) {
					int x = block.getX();
					if (Math.abs(x - woodsloc[0]) < 10) {
						for (int i = 0; i < woodsloc.length - 2; i += 3) {
							if (woodsloc[i] == x) {
								if (woodsloc[i + 1] == block.getY()) {
									if (woodsloc[i + 2] == block.getZ()) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	public boolean hasStairsBlock(Block block) {
		if (stairsloc != null) {
			if (spigot.getWorld() != null && spigot.getWorld().equals(block.getWorld())) {
				if (stairsloc.length > 2) {
					int x = block.getX();
					if (Math.abs(x - stairsloc[0]) < 10) {
						for (int i = 0; i < stairsloc.length - 2; i += 3) {
							if (stairsloc[i] == x) {
								if (stairsloc[i + 1] == block.getY()) {
									if (stairsloc[i + 2] == block.getZ()) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	// Returns true if the Offset of the clicked Sign matches the Barrel.
	// This prevents adding another sign to the barrel and clicking that.
	public boolean isSignOfBarrel(byte offset) {
		return offset == 0 || signoffset == 0 || signoffset == offset;
	}

	// Get the Barrel by Block, null if that block is not part of a barrel
	public static Barrel get(Block block) {
		if (block != null) {
			switch (block.getType()) {
			case OAK_FENCE:
			case NETHER_BRICK_FENCE:
			case SIGN:
			case WALL_SIGN:
			case ACACIA_FENCE:
			case BIRCH_FENCE:
			case DARK_OAK_FENCE:
			case IRON_BARS:
			case JUNGLE_FENCE:
			case SPRUCE_FENCE:
				Barrel barrel = getBySpigot(block);
				if (barrel != null) {
					return barrel;
				}
				return null;
			case OAK_PLANKS:
			case SPRUCE_PLANKS:
			case BIRCH_PLANKS:
			case JUNGLE_PLANKS:
			case ACACIA_PLANKS:
			case DARK_OAK_PLANKS:
			case OAK_STAIRS:
			case BIRCH_STAIRS:
			case JUNGLE_STAIRS:
			case SPRUCE_STAIRS:
			case ACACIA_STAIRS:
			case DARK_OAK_STAIRS:
				Barrel barrel2 = getByWood(block);
				if (barrel2 != null) {
					return barrel2;
				}
			default:
				break;
			}
		}
		return null;
	}

	// Get the Barrel by Sign or Spigot (Fastest)
	public static Barrel getBySpigot(Block sign) {
		// convert spigot if neccessary
		Block spigot = getSpigotOfSign(sign);

		byte signoffset = 0;
		if (!spigot.equals(sign)) {
			signoffset = (byte) (sign.getY() - spigot.getY());
		}

		for (Barrel barrel : barrels) {
			if (barrel.isSignOfBarrel(signoffset)) {
				if (barrel.spigot.equals(spigot)) {
					if (barrel.signoffset == 0 && signoffset != 0) {
						// Barrel has no signOffset even though we clicked a sign, may be old
						barrel.signoffset = signoffset;
					}
					return barrel;
				}
			}
		}
		return null;
	}

	// Get the barrel by its corpus (Wood Planks, Stairs)
	public static Barrel getByWood(Block wood) {
		if (isWood(wood.getType())) {
			for (Barrel barrel : barrels) {
				if (barrel.hasWoodBlock(wood)) {
					return barrel;
				}
			}
		} else if (isStairs(wood.getType())) {
			for (Barrel barrel : Barrel.barrels) {
				if (barrel.hasStairsBlock(wood)) {
					return barrel;
				}
			}
		}
		return null;
	}

	// creates a new Barrel out of a sign
	public static boolean create(Block sign, Player player) {
		Block spigot = getSpigotOfSign(sign);

		byte signoffset = 0;
		if (!spigot.equals(sign)) {
			signoffset = (byte) (sign.getY() - spigot.getY());
		}

		Barrel barrel = getBySpigot(spigot);
		if (barrel == null) {
			barrel = new Barrel(spigot, signoffset);
			if (barrel.getBrokenBlock(true) == null) {
				if (isSign(spigot)) {
					if (!player.hasPermission("brewery.createbarrel.small")) {
						Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Perms_NoSmallBarrelCreate"));
						return false;
					}
				} else {
					if (!player.hasPermission("brewery.createbarrel.big")) {
						Brewery.breweryDriver.msg(player, Brewery.breweryDriver.languageReader.get("Perms_NoBigBarrelCreate"));
						return false;
					}
				}
				barrels.add(barrel);
				return true;
			}
		} else {
			if (barrel.signoffset == 0 && signoffset != 0) {
				barrel.signoffset = signoffset;
				return true;
			}
		}
		return false;
	}

	// removes a barrel, throwing included potions to the ground
	public void remove(Block broken, Player breaker) {
		if (inventory != null) {
			for (HumanEntity human : inventory.getViewers()) {
				human.closeInventory();
			}
			ItemStack[] items = inventory.getContents();

			for (ItemStack item : items) {
				if (item != null) {
					// "broken" is the block that destroyed, throw them there!
					if (broken != null) {
						broken.getWorld().dropItem(broken.getLocation(), item);
					} else {
						spigot.getWorld().dropItem(spigot.getLocation(), item);
					}
				}
			}
		}

		barrels.remove(this);
	}

	//unloads barrels that are in a unloading world
	public static void onUnload(String name) {
		for (Barrel barrel : barrels) {
			if (barrel.spigot.getWorld().getName().equals(name)) {
				barrels.remove(barrel);
			}
		}
	}

	// If the Sign of a Large Barrel gets destroyed, set signOffset to 0
	public void destroySign() {
		signoffset = 0;
	}

	// Saves all data
	public static void save() {
		int id = 0;
		if (!barrels.isEmpty()) {
			
			for (Barrel barrel : barrels) {
				//Brewery.breweryDriver.debugLog("BARREL");
				//Location
				String location = Brewery.gson.toJson(barrel.spigot.getLocation().serialize());
				//Brewery.breweryDriver.debugLog(location);
				
				//woodloc
				String woodsloc = null;
				if(barrel.woodsloc != null) {
					woodsloc = Brewery.gson.toJson(barrel.woodsloc);
					//Brewery.breweryDriver.debugLog(woodsloc + " wood");
				}	
				
				//stairsloc
				String stairsloc = null;
				if(barrel.stairsloc != null) {
					stairsloc = Brewery.gson.toJson(barrel.stairsloc);
					//Brewery.breweryDriver.debugLog(stairsloc + " stairs");
				}
				
				//inventory
				String jsonInventory = BreweryUtils.toBase64(barrel.inventory);
				//Brewery.breweryDriver.debugLog(jsonInventory);
				
				String query = "REPLACE barrels SET idbarrels=?, location=?, woodsloc=?, stairsloc=?, signoffset=?, checked=?, inventory=?, time=?";
				try(PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
					stmt.setInt(1, id);
					stmt.setString(2, location);
					stmt.setString(3, woodsloc);
					stmt.setString(4, stairsloc);
					stmt.setByte(5, barrel.signoffset);
					stmt.setBoolean(6, barrel.checked);
					stmt.setString(7, jsonInventory);
					stmt.setFloat(8, barrel.time);
					
					//Brewery.breweryDriver.debugLog(stmt.toString());
					
					stmt.executeUpdate();
				} catch (SQLException e1) {
					e1.printStackTrace();
					return;
				}				
				id++;
			}
		}
		//clean up extras
		String query = "DELETE FROM barrels WHERE idbarrels >=?";
		try(PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setInt(1, id);
			//Brewery.breweryDriver.debugLog(stmt.toString());
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	// direction of the barrel from the spigot
	public static int getDirection(Block spigot) {
		int direction = 0;// 1=x+ 2=x- 3=z+ 4=z-
		Material type = spigot.getRelative(0, 0, 1).getType();
		if (isWood(type) || isStairs(type)) {
			direction = 3;
		}
		type = spigot.getRelative(0, 0, -1).getType();
		if (isWood(type) || isStairs(type)) {
			if (direction == 0) {
				direction = 4;
			} else {
				return 0;
			}
		}
		type = spigot.getRelative(1, 0, 0).getType();
		if (isWood(type) || isStairs(type)) {
			if (direction == 0) {
				direction = 1;
			} else {
				return 0;
			}
		}
		type = spigot.getRelative(-1, 0, 0).getType();
		if (isWood(type) || isStairs(type)) {
			if (direction == 0) {
				direction = 2;
			} else {
				return 0;
			}
		}
		return direction;
	}

	// is this a Large barrel?
	public boolean isLarge() {
		return !isSign(spigot);
	}

	// true for small barrels
	public static boolean isSign(Block spigot) {
		return spigot.getType() == Material.WALL_SIGN || spigot.getType() == Material.SIGN;
	}

	// woodtype of the block the spigot is attached to
	public byte getWood() {
		Block wood;
		switch (getDirection(spigot)) { // 1=x+ 2=x- 3=z+ 4=z-
			case 0:
				return 0;
			case 1:
				wood = spigot.getRelative(1, 0, 0);
				break;
			case 2:
				wood = spigot.getRelative(-1, 0, 0);
				break;
			case 3:
				wood = spigot.getRelative(0, 0, 1);
				break;
			default:
				wood = spigot.getRelative(0, 0, -1);
		}
		try {
			switch (wood.getType()) {
				case OAK_PLANKS:
				case OAK_STAIRS:
					return 2;
				case SPRUCE_PLANKS:
				case SPRUCE_STAIRS:
					return 4;
				case BIRCH_PLANKS:
				case BIRCH_STAIRS:
					return 1;
				case JUNGLE_PLANKS:
				case JUNGLE_STAIRS:
					return 3;
				case ACACIA_PLANKS:
				case ACACIA_STAIRS:
					return 5;
				case DARK_OAK_PLANKS:
				case DARK_OAK_STAIRS:
					return 6;
				default:
					return 0;
			}

		} catch (NoSuchFieldError | NoClassDefFoundError e) {
			// Using older minecraft versions some fields and classes do not exist
			return 0;
		}
	}

	// returns the Sign of a large barrel, the spigot if there is none
	public Block getSignOfSpigot() {
		if (signoffset != 0) {
			if (isSign(spigot)) {
				return spigot;
			}

			if (isSign(spigot.getRelative(0, signoffset, 0))) {
				return spigot.getRelative(0, signoffset, 0);
			} else {
				signoffset = 0;
			}
		}
		return spigot;
	}

	// returns the fence above/below a block, itself if there is none
	public static Block getSpigotOfSign(Block block) {

		int y = -2;
		while (y <= 1) {
			// Fence and Netherfence
			Block relative = block.getRelative(0, y, 0);
			if (isFence(relative.getType())) {
				return (relative);
			}
			y++;
		}
		return block;
	}

	public static boolean isStairs(Material material) {
		switch (material) {
			case OAK_STAIRS:
			case SPRUCE_STAIRS:
			case BIRCH_STAIRS:
			case JUNGLE_STAIRS:
			case ACACIA_STAIRS:
			case DARK_OAK_STAIRS:
				return true;
			default:
				return false;
		}
	}

	public static boolean isWood(Material material) {
		switch (material) {
			case OAK_PLANKS:
			case SPRUCE_PLANKS:
			case BIRCH_PLANKS:
			case JUNGLE_PLANKS:
			case ACACIA_PLANKS:
			case DARK_OAK_PLANKS:
				return true;
			default:
				return false;
		}
	}

	
	public static boolean isFence(Material material) {
		switch (material) {
			case OAK_FENCE:
			case NETHER_BRICK_FENCE:
			case ACACIA_FENCE:
			case BIRCH_FENCE:
			case DARK_OAK_FENCE:
			case IRON_BARS:
			case JUNGLE_FENCE:
			case SPRUCE_FENCE:
				return true;
			default:
				return false;
		}
	}

	// returns null if Barrel is correctly placed; the block that is missing when not
	// the barrel needs to be formed correctly
	// flag force to also check if chunk is not loaded
	public Block getBrokenBlock(boolean force) {
		if (force || spigot.getChunk().isLoaded()) {
			spigot = getSpigotOfSign(spigot);
			if (isSign(spigot)) {
				return checkSBarrel();
			} else {
				return checkLBarrel();
			}
		}
		return null;
	}

	public Block checkSBarrel() {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX;
		int startZ;
		int endX;
		int endZ;

		ArrayList<Integer> stairs = new ArrayList<Integer>();

		if (direction == 1) {
			startX = 1;
			endX = startX + 1;
			startZ = -1;
			endZ = 0;
		} else if (direction == 2) {
			startX = -2;
			endX = startX + 1;
			startZ = 0;
			endZ = 1;
		} else if (direction == 3) {
			startX = 0;
			endX = 1;
			startZ = 1;
			endZ = startZ + 1;
		} else {
			startX = -1;
			endX = 0;
			startZ = -2;
			endZ = startZ + 1;
		}

		Material type;
		int x = startX;
		int y = 0;
		int z = startZ;
		while (y <= 1) {
			while (x <= endX) {
				while (z <= endZ) {
					Block block = spigot.getRelative(x, y, z);
					type = block.getType();

					if (isStairs(type)) {
						if (y == 0) {
							// stairs have to be upside down
							
							Stairs stair = (Stairs) block.getBlockData();
							if(stair.getHalf() == Bisected.Half.BOTTOM) {
								return block;
							}
							/*MaterialData data = block.getState().getData();
							if (data instanceof Stairs) {
								if (!((Stairs) data).isInverted()) {
									return block;
								}
							}*/
							
						}
						stairs.add(block.getX());
						stairs.add(block.getY());
						stairs.add(block.getZ());
						z++;
					} else {
						return spigot.getRelative(x, y, z);
					}
				}
				z = startZ;
				x++;
			}
			z = startZ;
			x = startX;
			y++;
		}
		stairsloc = ArrayUtils.toPrimitive(stairs.toArray(new Integer[stairs.size()]));
		return null;
	}

	public Block checkLBarrel() {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX;
		int startZ;
		int endX;
		int endZ;

		ArrayList<Integer> stairs = new ArrayList<Integer>();
		ArrayList<Integer> woods = new ArrayList<Integer>();

		if (direction == 1) {
			startX = 1;
			endX = startX + 3;
			startZ = -1;
			endZ = 1;
		} else if (direction == 2) {
			startX = -4;
			endX = startX + 3;
			startZ = -1;
			endZ = 1;
		} else if (direction == 3) {
			startX = -1;
			endX = 1;
			startZ = 1;
			endZ = startZ + 3;
		} else {
			startX = -1;
			endX = 1;
			startZ = -4;
			endZ = startZ + 3;
		}

		Material type;
		int x = startX;
		int y = 0;
		int z = startZ;
		while (y <= 2) {
			while (x <= endX) {
				while (z <= endZ) {
					Block block = spigot.getRelative(x, y, z);
					type = block.getType();
					if (direction == 1 || direction == 2) {
						if (y == 1 && z == 0) {
							if (x == -1 || x == -4 || x == 1 || x == 4) {
								woods.add(block.getX());
								woods.add(block.getY());
								woods.add(block.getZ());
							}
							z++;
							continue;
						}
					} else {
						if (y == 1 && x == 0) {
							if (z == -1 || z == -4 || z == 1 || z == 4) {
								woods.add(block.getX());
								woods.add(block.getY());
								woods.add(block.getZ());
							}
							z++;
							continue;
						}
					}
					if (isWood(type) || isStairs(type)) {
						if (isWood(type)) {
							woods.add(block.getX());
							woods.add(block.getY());
							woods.add(block.getZ());
						} else {
							stairs.add(block.getX());
							stairs.add(block.getY());
							stairs.add(block.getZ());
						}
						z++;
					} else {
						return block;
					}
				}
				z = startZ;
				x++;
			}
			z = startZ;
			x = startX;
			y++;
		}
		stairsloc = ArrayUtils.toPrimitive(stairs.toArray(new Integer[stairs.size()]));
		woodsloc = ArrayUtils.toPrimitive(woods.toArray(new Integer[woods.size()]));

		return null;
	}

	public static class BarrelCheck extends BukkitRunnable {
		@Override
		public void run() {
			boolean repeat = true;
			while (repeat) {
				if (check < barrels.size()) {
					Barrel barrel = barrels.get(check);
					if (!barrel.checked) {
						Block broken = barrel.getBrokenBlock(false);
						if (broken != null) {
							Brewery.breweryDriver.debugLog("Barrel at " + broken.getWorld().getName() + "/" + broken.getX() + "/" + broken.getY() + "/" + broken.getZ()
									+ " has been destroyed unexpectedly, contents will drop");
							// remove the barrel if it was destroyed
							//barrel.willDestroy();
							barrel.remove(broken, null);
						} else {
							// Dont check this barrel again, its enough to check it once after every restart
							// as now this is only the backup if we dont register the barrel breaking, as sample
							// when removing it with some world editor
							barrel.checked = true;
						}
						repeat = false;
					}
					check++;
				} else {
					check = 0;
					repeat = false;
					cancel();
				}
			}
		}

	}

}
