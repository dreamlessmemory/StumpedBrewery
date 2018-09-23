package com.dreamless.brewery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BEffect {

	
	//private PotionEffectType type;
	private double potency = 0;
	private double duration = 0;
	private boolean hidden = false;
	private boolean randomEffect = false;
	private boolean randomDuration = false;
	private boolean randomLevel= false;
	Random rand = new Random();
	
	private static final int DURATION_CAP = 600;
	private static final int LEVEL_CAP_INSTANT = 20;
	private static final int LEVEL_CAP_DURATION = 5;
	private static final int DEFAULT_POTENCY = 1;
	private static final int DEFAULT_DURATION = 90;
	
	public BEffect() {
		this.potency = DEFAULT_POTENCY;
		this.duration = DEFAULT_DURATION;
	}
	
	public BEffect(double potency, double duration) {
		this.potency = potency;
		this.duration = duration;
	}

	public static ArrayList<PotionEffect> calculateEffect(HashMap<String, Double> aspects){
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
			Brewery.breweryDriver.debugLog("Processing - " + currentAspect);
			boolean isPotency = false;
			String trueAspect = currentAspect;
			PotionEffectType type;
			//Parse the PotionEffect name
			if(currentAspect.contains("_POTENCY")) {
				trueAspect = currentAspect.substring(0, currentAspect.length() - 8);
				isPotency = true;
			} else if (currentAspect.contains("_DURATION")) {
				trueAspect = currentAspect.substring(0, currentAspect.length() - 10);
				isPotency = false;
			}
			type = PotionEffectType.getByName(trueAspect);
			if(type != null){//It really is an effect.
				//Search for effect
				BEffect tempBEffect = effectMap.get(type);
				if(tempBEffect == null) {//doesn't exist yet
					tempBEffect = new BEffect();
				}
				if (isPotency) {//update potency
					tempBEffect.setPotency(calculatePotency(aspects.get(currentAspect), bonusPotency, type.isInstant()));
				} else { //update duration
					tempBEffect.setDuration(calculateDuration(aspects.get(currentAspect), bonusDuration));
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

	public static int calculateDuration(double duration, double bonusDuration) {
		return 180;
	}
	
	public static int calculatePotency(double potency, double bonusPotency, boolean isInstant) {
		return 2;
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
