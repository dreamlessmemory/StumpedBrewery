package com.dreamless.brewery.distillation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.dreamless.brewery.Brewery;
import com.dreamless.brewery.brew.BreweryEffectRequirement;
import com.dreamless.brewery.brew.Aspect;
import com.dreamless.brewery.utils.BreweryMessage;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.ItemLine;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;

public class BreweryDistiller implements InventoryHolder {
	
	public static ArrayList<BreweryDistiller> distillers = new ArrayList<BreweryDistiller>();
	public static HashMap<BreweryDistiller, Integer> runningDistillers = new HashMap<BreweryDistiller, Integer>();
	public static HashMap<BreweryDistiller, BukkitTask> preparingDistillers = new HashMap<BreweryDistiller, BukkitTask>();
	
	private static final int FILTER_LIMIT = 9;
	public static int DEFAULT_CYCLE_LENGTH = 40;
	
	private ArrayList<Material> filters = new ArrayList<Material>(); 
	private Block block;
	private Inventory filterInventory;
	private int filterCounter = 0;
	//private BrewerInventory brewingInventory;
	private boolean distilling = false;
	private boolean finishedDistilling = false;
	
	private Hologram hologram;
	private ItemLine filterLine;
	private TextLine statusLine;
	private TextLine secondStatusLine;


	public BreweryDistiller(Block block) {
		this.block = block;
		
		//Initialize Inventory
		filterInventory = org.bukkit.Bukkit.createInventory(this, FILTER_LIMIT, "Distiller Filter Cache");
		
		//Hologram
		if(hologram == null) {
			createHologram(block);
		}
		
		distillers.add(this);
	}
	
	
	//Static methods
	public static BreweryDistiller get(Block block) {
		for (BreweryDistiller distiller : distillers) {
			if (distiller.block.equals(block)) {
				return distiller;
			}
		}
		return null;
	}
	
	public static void add(BreweryDistiller distiller) {
		distillers.add(distiller);
	}
	
	public static void remove(Block block) {
		BreweryDistiller distiller = get(block);
		if (distiller != null) {
			for(ItemStack item: distiller.filterInventory) {
				distiller.ejectItem(item);
			}
			distillers.remove(distiller);
			
			//Remove hologram
			distiller.hologram.delete();
			
			//ruin brew if distilling
			if(distiller.distilling) {
				distiller.ruinPotions();
				Bukkit.getScheduler().cancelTask(runningDistillers.get(distiller));
			}
		}
	}
	
	public static boolean isValidFilter(Material material) {
		switch(material) {
			case DIORITE:
			case GRANITE:
			case ANDESITE:
			case GRAVEL:
			case SAND:
			case COBBLESTONE:
				return true;
			default:
				return false;
		}
	}
	
	public boolean addFilter(Material material) {
		if(filterCounter < FILTER_LIMIT) {
			filterInventory.addItem(new ItemStack(material));
			++filterCounter;
			return true;
		} else {
			return false;
		}
	}

	public void ejectAllFilters() {
		for(ItemStack item : filterInventory) {
			ejectItem(item);
		}
		filterInventory.clear();
	}
	
	public BreweryMessage startDistilling(Player player) {	
		if(finishedDistilling) {
			return new BreweryMessage(false, Brewery.getText("Distiller_Remove_Brews"));
		} else {
			distilling = true;			
			return new BreweryMessage(true, Brewery.getText("Distiller_Started"));
		}
	}
	public void removeSelf() {
		hologram.delete();
		distillers.remove(this);
		Brewery.breweryDriver.debugLog("Check distill list: " + distillers.size());
	}
	
	public void ruinPotions() {
		BrewerInventory brewingInventory = (BrewerInventory) ((InventoryHolder)block.getState()).getInventory();
		for(int i = 0; i < 3; i++) {
			ItemStack item = brewingInventory.getItem(i);
			if(item == null) continue;
			
			PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
			
			potionMeta.setDisplayName("Ruined Brew");
			
			ArrayList<String> agedFlavorText = new ArrayList<String>();
			agedFlavorText.add("A brew that was ruined");
			agedFlavorText.add("by being removed during distillation.");
			potionMeta.setLore(agedFlavorText);
			
			potionMeta.clearCustomEffects();
			
			item.setItemMeta(potionMeta);
			
			//Set NBT
			NBTItem nbti = new NBTItem(item);
			
			//Tag as distilling brew
			NBTCompound brewery = nbti.getCompound("brewery");
			brewery.setBoolean("ruined", true);
			
			brewingInventory.setItem(i, item);	
		}
	}
	
	private void createHologram(Block block) {
		Location above = block.getRelative(BlockFace.UP).getLocation();
		above.setX(above.getX()+ 0.5);
		above.setY(above.getY()+ 1.25);
		above.setZ(above.getZ()+ 0.5);
		hologram = HologramsAPI.createHologram(Brewery.breweryDriver, above);
		
		//Create ready message
		statusLine = hologram.appendTextLine("Distiller Ready");
		secondStatusLine = hologram.appendTextLine("Awaiting filters...");
	}

	private void ejectItem(ItemStack item) {
		if(item != null && item.getType() != Material.AIR)
		block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5), item);
		block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP,(float)(Math.random()/2) + 0.75f, (float)(Math.random()/2) + 0.75f);
	}


	private void finishDistilling() {
		distilling = false;
		finishedDistilling = true;
		
		BreweryAspectMatrix matrix = new BreweryAspectMatrix();
		
		for(ItemStack item : filterInventory) {
			if(item != null) {
				matrix.distillAspect(Aspect.getFilterAspect(item.getType()), item.getAmount());
			}
		}
		
		// TODO: Apply calculations now
		HashSet<PotionEffectType> effects = getPotionEffectTypes(matrix);
		
		//Set Hologram
		filterLine.setItemStack(new ItemStack(Material.POTION));
		statusLine.setText("Brews distilled.");
		secondStatusLine.setText("Remove brews");
		
		//Effects
		//Sound and particle effects
		block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().getX() + 0.5, block.getLocation().getY() + 1.5, block.getLocation().getZ() + 0.5, 10, 0.5, 0.5, 0.5);
		block.getWorld().playSound(block.getLocation(), Sound.ITEM_BOTTLE_FILL, (float)(Math.random()/8) + 0.1f, (float)(Math.random()/2) + 0.75f);
	}

	@Override
	public Inventory getInventory() {
		return filterInventory;
	}
	
	public boolean isDistilling() {
		return distilling;
	}
	
	public boolean isFinishedDistilling() {
		return finishedDistilling;
	}
	
	// TODO: Implementation
	public static HashSet<PotionEffectType> getPotionEffectTypes(BreweryAspectMatrix matrix){
		HashSet<PotionEffectType> set = new HashSet<PotionEffectType>();
		set.add(PotionEffectType.GLOWING);
		return set;
	}
	
	public static class DistillerRunnable extends BukkitRunnable {
		private final int cycles;
		private final int cycleLength;
		private int currentCycle = 1;
		private int currentTime = 0;
		private BreweryDistiller distiller;
		
		public DistillerRunnable(int cycleLength, BreweryDistiller distiller) {
			cycles = distiller.filters.size();
			this.cycleLength = cycleLength;
			this.distiller = distiller;
			
		}
		
		@Override
		public void run() {
			//Bubble effects
			if(Math.random() > Brewery.effectLevel) {
				distiller.block.getWorld().spawnParticle(Particle.SMOKE_NORMAL, distiller.block.getLocation().getX() + 0.5, distiller.block.getLocation().getY() + 1.5, distiller.block.getLocation().getZ() + 0.5, 20, 0.15, 0.15, 0.15, 0.05);
				distiller.block.getWorld().playSound(distiller.block.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, (float)(Math.random()/8) + 0.1f, (float)(Math.random() * 1.5) + 0.5f);
			}
			
			if(++currentTime < cycleLength) {
				//Update Hologram
				distiller.secondStatusLine.setText("Cycle " +  currentCycle + "/" + cycles + " : " + (cycleLength - currentTime) + " s remaining");
				
			} else {
				//increment cycles
				currentTime = 0;
				currentCycle +=1;
				if(currentCycle > cycles) {
					distiller.finishDistilling();
					Brewery.breweryDriver.debugLog("End distill");
					this.cancel();
				} else {
					distiller.filterLine.setItemStack(new ItemStack(distiller.filters.get(0)));
					distiller.statusLine.setText("Filter: " + WordUtils.capitalize((distiller.filters.get(0).toString().toLowerCase().replace("_", " "))));
					distiller.secondStatusLine.setText("Cycle " +  currentCycle + "/" + cycles + " : " + (cycleLength - currentTime) + " s remaining");
				}
			}
		}
	}

	public class BreweryAspectMatrix{
		private HashMap<Aspect, Integer> aspectMatrix;
		private int totalCount = BreweryEffectRequirement.MAXIMUM_TOTAL_STACKS;
		public BreweryAspectMatrix() {
			aspectMatrix.put(Aspect.LITHIC, 3);
			aspectMatrix.put(Aspect.INFERNAL, 3);
			aspectMatrix.put(Aspect.PYROTIC, 3);
			aspectMatrix.put(Aspect.AERIAL, 3);
			aspectMatrix.put(Aspect.VOID, 3);
			aspectMatrix.put(Aspect.AQUATIC, 3);
		}
		public int getTotalCount() {
			return totalCount;
		}
		public void distillAspect(Aspect aspect, int amount) {
			aspectMatrix.put(aspect, Math.max(aspectMatrix.get(aspect) - amount, 0));
		}
	}
}
