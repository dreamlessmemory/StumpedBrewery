package com.dreamless.brewery.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dreamless.brewery.Brewery;

public class BEffect {

	
	//private PotionEffectType type;
	private double potency = 0;
	private double duration = 0;
	private boolean hidden = false;
	Random rand = new Random();
	
	private static final int DURATION_CAP = 1245;
	private static final int LEVEL_CAP_INSTANT = 5;
	private static final int LEVEL_CAP_DURATION = 4;
	private static final int DEFAULT_POTENCY = 0;
	private static final int MINUMUM_DURATION = 45;
	private static final int MAX_SCORE = 100;
	private static final double CONTROL = 1.0;
	
	public BEffect() {
		this.potency = DEFAULT_POTENCY;
		this.duration = MINUMUM_DURATION;
	}
	
	public BEffect(double potency, double duration) {
		this.potency = potency;
		this.duration = duration;
	}

	public static ArrayList<PotionEffect> calculateEffect(HashMap<String, Double> aspects, int potencyMultiplier, int durationMultiplier){
		ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
		HashMap<PotionEffectType, BEffect> effectMap = new HashMap<PotionEffectType, BEffect>();
		
		double bonusPotency = 0;
		double bonusDuration = 0;
		if(aspects.containsKey("RAW_POTENCY")) {
			bonusPotency = aspects.remove("RAW_POTENCY");
		}
		if(aspects.containsKey("RAW_DURATION")) {
			bonusPotency = aspects.remove("RAW_DURATION");
		}
		
		for(String currentAspect : aspects.keySet()) {
			//Brewery.breweryDriver.debugLog("Processing - " + currentAspect);
			boolean isPotency = false;
			String trueAspect = currentAspect;
			PotionEffectType type;
			//Parse the PotionEffect name
			if(currentAspect.contains("_POTENCY")) {
				trueAspect = currentAspect.substring(0, currentAspect.length() - 8);
				isPotency = true;
			} else if (currentAspect.contains("_DURATION")) {
				trueAspect = currentAspect.substring(0, currentAspect.length() - 9);
				isPotency = false;
			} else { //neither
				Brewery.breweryDriver.debugLog("Skipping effect processing - " + trueAspect);
			}
			type = PotionEffectType.getByName(trueAspect);
			if(type != null){//It really is an effect.
				//Search for effect
				BEffect tempBEffect = effectMap.get(type);
				if(tempBEffect == null) {//doesn't exist yet
					Brewery.breweryDriver.debugLog("Making new effect for " + currentAspect);
					tempBEffect = new BEffect();
				}
				if (isPotency) {//update potency
					Brewery.breweryDriver.debugLog("Updating potency for " + currentAspect);
					tempBEffect.setPotency(calculatePotency(aspects.get(currentAspect), bonusPotency, type.isInstant(), potencyMultiplier));
				} else {//update duration
					Brewery.breweryDriver.debugLog("Updating duration for " + currentAspect);
					tempBEffect.setDuration(calculateDuration(aspects.get(currentAspect), bonusDuration, durationMultiplier));
				}
				effectMap.put(type, tempBEffect);
			}
		}
		
		for(PotionEffectType effectType: effectMap.keySet()) {
			BEffect effect = effectMap.get(effectType);
			effects.add(effectType.createEffect(((int)effect.getDuration()) * 20, (int)effect.getPotency()));
		}
		
		return effects;
	}

	public static int calculateDuration(double duration, double bonusDuration, int durationMultiplier) {
		double difference = DURATION_CAP - MINUMUM_DURATION;
		double score = (duration + bonusDuration);
		score = (score * durationMultiplier) / 100;
		double scaledScore = Math.ceil(Math.atan((score /MAX_SCORE) * CONTROL) * difference);
		return MINUMUM_DURATION + (int)scaledScore;
	}
	
	public static int calculatePotency(double potency, double bonusPotency, boolean isInstant, int potencyMultiplier) {
		double score = (potency + bonusPotency);
		score = (score* potencyMultiplier) / (MAX_SCORE * 100);
		int calculatedScore = (int)Math.ceil(Math.atan(CONTROL * score) * (isInstant ? LEVEL_CAP_INSTANT : LEVEL_CAP_DURATION));
		return Math.max(0, calculatedScore - 1);
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public double getPotency() {
		return potency;
	}

	public void setPotency(double potency) {
		this.potency = potency;
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}
}
