package com.dreamless.brewery;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

public class BEffect {

	
	private PotionEffectType type;
	private short minlvl;
	private short maxlvl;
	private short minduration;
	private short maxduration;
	private boolean hidden = false;
	private boolean randomEffect = false;
	private boolean randomDuration = false;
	private boolean randomLevel= false;
	Random rand = new Random();
	
	private static final int DURATION_CAP = 600;
	private static final int LEVEL_CAP_INSTANT = 20;
	private static final int LEVEL_CAP_DURATION = 5;
	
	public BEffect(String effectString) {
		String[] effectSplit = effectString.split("/");
		String effect = effectSplit[0];
		if (effect.equalsIgnoreCase("WEAKNESS") ||
				effect.equalsIgnoreCase("INCREASE_DAMAGE") ||
				effect.equalsIgnoreCase("SLOW") ||
				effect.equalsIgnoreCase("SPEED") ||
				effect.equalsIgnoreCase("REGENERATION")) {
			// hide these effects as they put crap into lore
			// Dont write Regeneration into Lore, its already there storing data!
			hidden = true;
		} else if (effect.endsWith("X")) {
			hidden = true;
			effect = effect.substring(0, effect.length() - 1);
		}
		
		if(effect.equalsIgnoreCase("RANDOM")) { //If random, default to speed
			randomEffect = true;
			type = PotionEffectType.getByName("SPEED");
		} else {
			type = PotionEffectType.getByName(effect);
			if (type == null) {
				Brewery.breweryDriver.errorLog("Effect: " + effect + " does not exist!");
				return;
			}
		}

		if (effectSplit.length == 3) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				if(range[0].equalsIgnoreCase("RANDOM")) {
					range[0] = LEVEL_CAP_INSTANT + "";
					randomLevel= true;
				}
				setLvl(range); //level
			} else {
				if(range[0].equalsIgnoreCase("RANDOM")) {
					range[0] = LEVEL_CAP_DURATION + "";
					randomLevel= true;
				}
				setLvl(range); //level
				range = effectSplit[2].split("-");
				if(range[0].equalsIgnoreCase("RANDOM")) {
					range[0] = DURATION_CAP + "";
					randomDuration = true;
				}
				setDuration(range); //duration
			}
		} else if (effectSplit.length == 2) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				if(range[0].equalsIgnoreCase("RANDOM")) {
					range[0] = LEVEL_CAP_INSTANT + "";
					randomLevel= true;
				}
				setLvl(range);
			} else {
				if(range[0].equalsIgnoreCase("RANDOM")) {
					range[0] = DURATION_CAP + "";
					randomDuration = true;
				}
				setDuration(range);
				maxlvl = 3;
				minlvl = 1;
			}
		} else {
			maxduration = 20;
			minduration = 10;
			maxlvl = 3;
			minlvl = 1;
		}
	}

	private void setLvl(String[] range) {
		if (range.length == 1) {
			maxlvl = (short) Brewery.breweryDriver.parseInt(range[0]);
			minlvl = 1;
		} else {
			maxlvl = (short) Brewery.breweryDriver.parseInt(range[1]);
			minlvl = (short) Brewery.breweryDriver.parseInt(range[0]);
		}
	}

	private void setDuration(String[] range) {
		if (range.length == 1) {
			maxduration = (short) Brewery.breweryDriver.parseInt(range[0]);
			minduration = (short) (maxduration / 8);
		} else {
			maxduration = (short) Brewery.breweryDriver.parseInt(range[1]);
			minduration = (short) Brewery.breweryDriver.parseInt(range[0]);
		}
	}

	public void apply(int quality, Player player) {
		int duration = calcDuration(quality);
		int lvl = calcLvl(quality);
		
		if(randomEffect) {
			PotionEffectType[] possibilities = PotionEffectType.values(); 
			do {
				type = possibilities[rand.nextInt(possibilities.length)];
			} while (type == null);
		}

		if (lvl < 1 || (duration < 1 && !type.isInstant())) {
			return;
		}

		duration *= 20;
		duration /= type.getDurationModifier();
		type.createEffect(duration, lvl - 1).apply(player);
	}

	public int calcDuration(float quality) {
		int effectiveMaxDuration = randomDuration ? (int)(rand.nextDouble() * maxduration) : maxduration;
		return (int) Math.round(minduration + ((effectiveMaxDuration - minduration) * (quality / 10.0)));
	}

	public int calcLvl(float quality) {
		int effectiveMaxLevel = randomLevel ? (int)(rand.nextDouble() * maxlvl) : maxlvl;
		return (int) Math.round(minlvl + ((effectiveMaxLevel - minlvl) * (quality / 10.0)));
	}

	public void writeInto(PotionMeta meta, int quality) {
		if ((calcDuration(quality) > 0 || type.isInstant()) && calcLvl(quality) > 0) {
			meta.addCustomEffect(type.createEffect(0, 0), true);
		} else {
			meta.removeCustomEffect(type);
		}
	}

	public boolean isValid() {
		return type != null && minlvl >= 0 && maxlvl >= 0 && minduration >= 0 && maxduration >= 0;
	}

	public boolean isHidden() {
		return hidden;
	}
}
