package com.dreamless.brewery.entity;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.MashBucket;
import com.dreamless.brewery.utils.BreweryMessage;

public class BreweryCauldron /*implements InventoryHolder */ {
	private static CopyOnWriteArrayList<BreweryCauldron> cauldronList = new CopyOnWriteArrayList<BreweryCauldron>();
	private static HashMap<BreweryCauldron, BukkitTask> taskMap = new HashMap<>();
	private static int totalCookTime = 5;

	private final Block block;
	private final MashBucket mashBucket;
	private int cookTime = 0; // Seconds
	private boolean cooking = true;
	//private Hologram hologram;
	//private Inventory inventory;

	public BreweryCauldron(Block block, ItemStack bucket) {
		this.block = block;
		//inventory = org.bukkit.Bukkit.createInventory(this, 9, "Brewery Cauldron");
		mashBucket = new MashBucket(bucket);
		cauldronList.add(this);
//		if(Brewery.hologramsEnabled)
//		{
//			createHologram(block);
//			updateHologram(HologramActions.INIT);
//		}
		startCooking();
	}

	// Retrieve cauldron from list by Block
	public static BreweryCauldron get(Block block) {
		for (BreweryCauldron bcauldron : cauldronList) {
			if (bcauldron.block.equals(block)) {
				return bcauldron;
			}
		}
		return null;
	}

	// reset to normal cauldron
	public static void remove(Block block) {
		BreweryCauldron bcauldron = get(block);
		if (bcauldron != null) {
			Brewery.breweryDriver.debugLog("Removing Cauldron");
			BukkitTask task = taskMap.get(bcauldron);
			if(task != null) 
			{
				Brewery.breweryDriver.debugLog("Task stopped");
				task.cancel();
			}
			if (!bcauldron.cooking) {
				bcauldron.dumpContents();
			}
			cauldronList.remove(bcauldron);

			// Remove hologram
//			if(bcauldron.hologram != null)
//			{			
//				bcauldron.hologram.delete();
//			}
		}
	}

	public static void remove(BreweryCauldron bcauldron) {
		if (!bcauldron.cooking) {
			bcauldron.dumpContents();
		}
		Brewery.breweryDriver.debugLog("Removing Cauldron");
		BukkitTask task = taskMap.get(bcauldron);
		if(task != null) 
		{
			Brewery.breweryDriver.debugLog("Task stopped");
			task.cancel();
		}
		cauldronList.remove(bcauldron);

		// Remove hologram
//		if(bcauldron.hologram != null)
//		{			
//			bcauldron.hologram.delete();
//		}
	}

	// unloads cauldrons that are in a unloading world
	// as they were written to file just before, this is safe to do
	public static void onUnload(String name) {
		for (BreweryCauldron bcauldron : cauldronList) {
			if (bcauldron.block.getWorld().getName().equals(name)) {
				cauldronList.remove(bcauldron);
			}
		}
	}

	public static void onDisable() {
		cauldronList.clear();
	}

	public static boolean isUseableCauldron (Block block) {
		Material down = block.getRelative(BlockFace.DOWN).getType();
		Material up = block.getRelative(BlockFace.UP).getType();
	
		if (down != Material.FIRE && down != Material.LAVA && down != Material.MAGMA_BLOCK
				&& down != Material.CAMPFIRE) {
			return false;
		}
	
		if (up != Material.AIR) {
			return false;
		}
		return true;
	}

	// fills players bottle with cooked brew
	public ItemStack getFermentedBucket() {
		removeSelf();
		block.setType(Material.CAULDRON);
		return mashBucket.getFermentedItem();
	}

	// prints the current cooking time to the player
	public void printTime(Player player) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Error_NoPermissions"));
			return;
		}

		if (cookTime > 1) {
			Brewery.breweryDriver.msg(player,Brewery.getText("Player_CauldronInfo2", Integer.toString(cookTime)));
		} else if (cookTime == 1) {
			Brewery.breweryDriver.msg(player, Brewery.getText("Player_CauldronInfo1"));
		} else {
			Brewery.breweryDriver.msg(player, Brewery.getText("Player_CauldronInfo0"));
		}
	}


	// 0 = empty, 1 = something in, 2 = full
	public int getFillLevel() {
		if(block.getType() == Material.CAULDRON)
		{
			return 0;
		}
		else {
			return ((Levelled)block.getState().getBlockData()).getLevel();	
		}
	}

	public BreweryMessage startCooking() {
		// Set Feedback effects
		block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().getX() + 0.5,
				block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);

		// Manage parameters
		cooking = true;

		//Bukkit.getScheduler().runTaskTimer(Brewery.breweryDriver,
		//		new BreweryCauldronRunnable(this), 0, 20);
		taskMap.put(this, Bukkit.getScheduler().runTaskTimer(Brewery.breweryDriver,
				new BreweryCauldronRunnable(this), 0, 20));

		// Create hologram
//		if (hologram == null && Brewery.hologramsEnabled) {
//			createHologram(block);
//			updateHologram(HologramActions.START_COOKING);
//		}

		// Return
		return new BreweryMessage(true, Brewery.getText("Fermentation_Start_Fermenting"));
	}

	public void purgeContents() {
		for (ItemStack item : mashBucket.getContents()) {
				dumpItem(item);
		}
	}

	public void onUpdate() {
		// Check if fire still alive
		if ((!block.getChunk().isLoaded() || fireAndAirInPlace().getResult()) && cooking) {

			if (getFillLevel() == 0) {// remove yourself if empty
				remove(block);
				return;
			}

			// Update Sign
			//updateHologram(HologramActions.TICK);

			++cookTime;
			// Bubble effects
			if (Math.random() > Brewery.effectLevel) {
				block.getWorld().spawnParticle(Particle.SMOKE_NORMAL, block.getLocation().getX() + 0.5,
						block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 20, 0.15, 0.15, 0.15, 0.05);
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BREWING_STAND_BREW,
						(float) (Math.random() / 8) + 0.1f, (float) (Math.random() * 1.5) + 0.5f);
			} 
			// Every minute larger explosion
			if (cookTime % 60 == 0) {
				// Sound and particle effects
				block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().getX() + 0.5,
						block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE,
						(float) (Math.random() / 8) + 0.1f, (float) (Math.random() / 2) + 0.75f);
			}
			
			// Stop cooking
			if(cookTime >= totalCookTime)
			{
				cooking = false;
				block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().getX() + 0.5,
						block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
				block.getWorld().playSound(block.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
			}
		} 
		else if (cookTime >= totalCookTime != cooking)
		{
			block.getWorld().spawnParticle(Particle.BUBBLE_POP, block.getLocation().getX() + 0.5,
					block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
			block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP,
					(float) (Math.random() / 8) + 0.1f, (float) (Math.random() / 2) + 0.75f);
		}
		else 
		{ // no fire, stop cooking
			if (cooking) {
				cooking = false;
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 2.0f, 1.0f);
				//updateHologram(HologramActions.STOP_COOKING);
				//hologram.appendTextLine("Fermentation stopped");
			}
		}
	}
	
	public void removeSelf()
	{
		// Remove from tasks
		BukkitTask task = taskMap.get(this);
		if(task != null) 
		{
			Brewery.breweryDriver.debugLog("Task stopped");
			task.cancel();
		}
		
		// Remove from cauldrons
		cauldronList.remove(this);
	}

	/**
	 * PRIVATE METHODS
	 */

	private BreweryMessage fireAndAirInPlace() {

		Material down = block.getRelative(BlockFace.DOWN).getType();
		Material up = block.getRelative(BlockFace.UP).getType();

		if (down != Material.FIRE && down != Material.LAVA && down != Material.MAGMA_BLOCK
				&& down != Material.CAMPFIRE) {
			return new BreweryMessage(false, Brewery.getText("Fermentation_No_Heat"));
		}

		if (up != Material.AIR) {
			return new BreweryMessage(false, Brewery.getText("Fermentation_No_Space_Above"));
		}

		return new BreweryMessage(true, "");
	}

//	private void createHologram(Block block) {
//		Location above = block.getRelative(BlockFace.UP).getLocation();
//		above.setX(above.getX() + 0.5);
//		above.setY(above.getY() + 0.75);
//		above.setZ(above.getZ() + 0.5);
//		hologram = HologramsAPI.createHologram(Brewery.breweryDriver, above);
//	}

//	private void updateHologram(HologramActions action) {
//		if(!Brewery.hologramsEnabled)
//		{
//			return;
//		}
//
//		if(action == HologramActions.TICK) {// Time
//			int secondsCooked = cookTime % 60;
//			((TextLine)hologram.getLine(0)).setText(cookTime/60 + ":" + (secondsCooked < 10 ? "0" : "") + secondsCooked);
//		}	else if (action == HologramActions.START_COOKING) {
//			((TextLine)hologram.getLine(1)).setText("Cooking...");
//		} else if (action == HologramActions.STOP_COOKING) { // 
//			((TextLine)hologram.getLine(1)).setText("Cooking Stopped");
//		} else if (action == HologramActions.ADD_INGREDIENT) { // Inventory
//			// Clean out past lines
//			for (int i = hologram.size()-1; i >= 2; i--){
//				hologram.removeLine(i);
//			}
//
//			// Add new Ones
//			for (ItemStack itemStack : inventory.getContents()) {
//				if (itemStack != null) {
//					hologram.appendTextLine(BreweryUtils.getItemName(itemStack) + " x" + itemStack.getAmount());
//				}
//			}
//			// Location above = block.getRelative(BlockFace.UP).getLocation().add(0.5, 0.75
//			// + (hologram.size()-2) * 0.25, 0.5);
//			hologram.teleport(
//					block.getRelative(BlockFace.UP).getLocation().add(0.5, 0.75 + (hologram.size() - 2) * 0.25, 0.5));
//		} else if (action == HologramActions.INIT) {
//			hologram.clearLines();
//			hologram.appendTextLine("0:00");
//			hologram.appendTextLine("Ready...");
//		}
//	}

	private void dumpContents() {
		for (ItemStack item : mashBucket.getContents()) {
			if (item != null) {
				dumpItem(item);
			}
		}
	}

	private void dumpItem(ItemStack item) {
		if (item != null) {
			block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5), item);
			block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP,
					(float) (Math.random() / 2) + 0.75f, (float) (Math.random() / 2) + 0.75f);
		}
	}

	public class BreweryCauldronRunnable implements Runnable {

		private final BreweryCauldron cauldron;

		public BreweryCauldronRunnable(BreweryCauldron cauldron) {
			this.cauldron = cauldron;
		}

		@Override
		public void run() {
			cauldron.onUpdate();
		}
	}

//	private enum HologramActions{
//		INIT, START_COOKING, ADD_INGREDIENT, TICK, STOP_COOKING;
//	}
}
