package com.dreamless.brewery;

import org.bukkit.Material;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Brew {

	// represents the liquid in the brewed Potions

	public static Map<Integer, Brew> potions = new HashMap<Integer, Brew>();
	public static long installTime = System.currentTimeMillis(); // plugin install time in millis after epoch
	public static Boolean colorInBarrels; // color the Lore while in Barrels
	public static Boolean colorInBrewer; // color the Lore while in Brewer

	private BIngredients ingredients;
	private int quality;
	private int distillRuns;
	private float ageTime;
	private float wood;
	private BRecipe currentRecipe;
	private boolean unlabeled;
	private boolean persistent;
	private int lastUpdate; // last update in hours after install time

	public Brew(int uid, BIngredients ingredients) {
		this.ingredients = ingredients;
		touch();
		potions.put(uid, this);
	}

	// quality already set
	public Brew(int uid, int quality, BRecipe recipe, BIngredients ingredients) {
		this.ingredients = ingredients;
		this.quality = quality;
		this.currentRecipe = recipe;
		touch();
		potions.put(uid, this);
	}

	// loading from file
	public Brew(int uid, BIngredients ingredients, int quality, int distillRuns, float ageTime, float wood, String recipe, boolean unlabeled, boolean persistent, boolean stat, int lastUpdate) {
		potions.put(uid, this);
		this.ingredients = ingredients;
		this.quality = quality;
		this.distillRuns = distillRuns;
		this.ageTime = ageTime;
		this.wood = wood;
		this.unlabeled = unlabeled;
		this.persistent = persistent;
		this.lastUpdate = lastUpdate;
		setRecipeFromString(recipe);
	}

	// returns a Brew by its UID
	public static Brew get(int uid) {
		if (uid < -1) {
			if (!potions.containsKey(uid)) {
				Brewery.breweryDriver.errorLog("Database failure! unable to find UID " + uid + " of a custom Potion!");
				return null;// throw some exception?
			}
		} else {
			return null;
		}
		return potions.get(uid);
	}

	// returns a Brew by PotionMeta
	public static Brew get(PotionMeta meta) {
		return get(getUID(meta));
	}

	// returns a Brew by ItemStack
	public static Brew get(ItemStack item) {
		if (item.getType() == Material.POTION) {
			if (item.hasItemMeta()) {
				return get((PotionMeta) item.getItemMeta());
			}
		}
		return null;
	}

	// returns UID of custom Potion item
	public static int getUID(ItemStack item) {
		return getUID((PotionMeta) item.getItemMeta());
	}

	// returns UID of custom Potion meta
	public static int getUID(PotionMeta potionMeta) {
		if (potionMeta.hasCustomEffect(PotionEffectType.REGENERATION)) {
			for (PotionEffect effect : potionMeta.getCustomEffects()) {
				if (effect.getType().equals(PotionEffectType.REGENERATION)) {
					if (effect.getDuration() < -1) {
						return effect.getDuration();
					}
				}
			}
		}
		return 0;
	}

	// generate an UID
	public static int generateUID() {
		int uid = -2;
		while (potions.containsKey(uid)) {
			uid -= 1;
		}
		return uid;
	}

	//returns the recipe with the given name, recalculates if not found
	public boolean setRecipeFromString(String name) {
		/* currentRecipe = null;
		if (name != null && !name.equals("")) {
			for (BRecipe recipe : BRecipe.recipes) {
				if (recipe.getName().equalsIgnoreCase(name)) {
					currentRecipe = recipe;
					return true;
				}
			}

			if (quality > 0) {
				currentRecipe = ingredients.getBestRecipe(wood, ageTime, distillRuns > 0);
				if (currentRecipe != null) {
					if (!stat) {
						this.quality = calcQuality();
					}
					P.p.log("Brew was made from Recipe: '" + name + "' which could not be found. '" + currentRecipe.getName() + "' used instead!");
					return true;
				} else {
					P.p.errorLog("Brew was made from Recipe: '" + name + "' which could not be found!");
				}
			}
		}*/
		return false;
	}

	public boolean reloadRecipe() {
		return currentRecipe == null || setRecipeFromString(currentRecipe.getName());
	}

	// Copy a Brew with a new unique ID and return its item
	public ItemStack copy(ItemStack item) {
		ItemStack copy = item.clone();
		int uid = generateUID();
		clone(uid);
		PotionMeta meta = (PotionMeta) copy.getItemMeta();
		meta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);
		copy.setItemMeta(meta);
		return copy;
	}

	// Clones this instance with a new unique ID
	public Brew clone(int uid) {
		Brew brew = new Brew(uid, quality, currentRecipe, ingredients);
		brew.distillRuns = distillRuns;
		brew.ageTime = ageTime;
		brew.unlabeled = unlabeled;
		return brew;
	}

	// remove potion from file (drinking, despawning, combusting, cmdDeleting, should be more!)
	public void remove(ItemStack item) {
		if (!persistent) {
			potions.remove(getUID(item));
		}
	}

	// calculate alcohol from recipe
	public int calcAlcohol() {
		if (quality == 0) {
			// Give bad potions some alc
			int badAlc = 0;
			if (distillRuns > 1) {
				badAlc = distillRuns;
			}
			if (ageTime > 10) {
				badAlc += 5;
			} else if (ageTime > 2) {
				badAlc += 3;
			}
			if (currentRecipe != null) {
				return badAlc;
			} else {
				return badAlc / 2;
			}
		}

		if (currentRecipe != null) {
			
		}
		return 0;
	}

	// Set unlabeled to true to hide the numbers in Lore
	public void unLabel(ItemStack item) {
		PotionMeta meta = (PotionMeta) item.getItemMeta();
		if (meta.hasLore()) {
			if (distillRuns > 0) {
				addOrReplaceLore(meta, Brewery.breweryDriver.color("&7"), Brewery.breweryDriver.languageReader.get("Brew_Distilled"));
			}
			if (ageTime >= 1) {
				addOrReplaceLore(meta, Brewery.breweryDriver.color("&7"), Brewery.breweryDriver.languageReader.get("Brew_BarrelRiped"));
			}
			item.setItemMeta(meta);
		}
		unlabeled = true;
	}

	// Do some regular updates
	public void touch() {
		lastUpdate = (int) ((double) (System.currentTimeMillis() - installTime) / 3600000D);
	}

	public int getDistillRuns() {
		return distillRuns;
	}

	public float getAgeTime() {
		return ageTime;
	}

	public BRecipe getCurrentRecipe() {
		return currentRecipe;
	}

	public boolean isPersistent() {
		return persistent;
	}

	// Make a potion persistent to not delete it when drinking it
	public void makePersistent() {
		persistent = true;
	}

	// Remove the Persistence Flag from a brew, so it will be normally deleted when drinking it
	public void removePersistence() {
		persistent = false;
	}


	public int getLastUpdate() {
		return lastUpdate;
	}

	// Distilling section ---------------

	// distill all custom potions in the brewer
	public static void distillAll(BrewerInventory inv, Brew[] contents) {
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] != null) {
				ItemStack slotItem = inv.getItem(slot);
				PotionMeta potionMeta = (PotionMeta) slotItem.getItemMeta();
				contents[slot].distillSlot(slotItem, potionMeta);
			}
		}
	}

	// distill custom potion in given slot
	public void distillSlot(ItemStack slotItem, PotionMeta potionMeta) {//TODO Update
		distillRuns += 1;
		touch();

		slotItem.setItemMeta(potionMeta);
	}

	// Ageing Section ------------------

	public void age(ItemStack item, float time, byte woodType) {//TODO: Aging calculation

		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		ageTime += time;

		// if younger than half a day, it shouldnt get aged form
		if (ageTime > 0.5) {
		}

		// Lore
		
		touch();
		item.setItemMeta(potionMeta);
	}
	// Adds or replaces a line of Lore. Searches for Substring lore and replaces it
	public static void addOrReplaceLore(PotionMeta meta, String prefix, String lore) {
		if (meta.hasLore()) {
			List<String> existingLore = meta.getLore();
			int index = indexOfSubstring(existingLore, lore);
			if (index > -1) {
				existingLore.set(index, prefix + lore);
			} else {
				existingLore.add(prefix + lore);
			}
			meta.setLore(existingLore);
			return;
		}
		List<String> newLore = new ArrayList<String>();
		newLore.add("");
		newLore.add(prefix + lore);
		meta.setLore(newLore);
	}

	// Removes all effects except regeneration which stores data
	public static void removeEffects(PotionMeta meta) {
		if (meta.hasCustomEffects()) {
			for (PotionEffect effect : meta.getCustomEffects()) {
				PotionEffectType type = effect.getType();
				if (!type.equals(PotionEffectType.REGENERATION)) {
					meta.removeCustomEffect(type);
				}
			}
		}
	}

	// Returns the Index of a String from the list that contains this substring
	public static int indexOfSubstring(List<String> list, String substring) {
		for (int index = 0; index < list.size(); index++) {
			String string = list.get(index);
			if (string.contains(substring)) {
				return index;
			}
		}
		return -1;
	}

	// True if the PotionMeta has colored Lore
	public static Boolean hasColorLore(PotionMeta meta) {
		return meta.hasLore() && !meta.getLore().get(1).startsWith(Brewery.breweryDriver.color("&7"));
	}

	// gets the Color that represents a quality in Lore
	public static String getQualityColor(int quality) {
		String color;
		if (quality > 8) {
			color = "&a";
		} else if (quality > 6) {
			color = "&e";
		} else if (quality > 4) {
			color = "&6";
		} else if (quality > 2) {
			color = "&c";
		} else {
			color = "&4";
		}
		return Brewery.breweryDriver.color(color);
	}
}
