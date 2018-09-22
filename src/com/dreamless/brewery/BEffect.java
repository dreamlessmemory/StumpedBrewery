package com.dreamless.brewery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BEffect {

	
	private PotionEffectType type;
	private double potencyS = 0;
	private double durationS = 0;
	private boolean hidden = false;
	private boolean randomEffect = false;
	private boolean randomDuration = false;
	private boolean randomLevel= false;
	Random rand = new Random();
	
	private static final int DURATION_CAP = 600;
	private static final int LEVEL_CAP_INSTANT = 20;
	private static final int LEVEL_CAP_DURATION = 5;
	
	public BEffect(PotionEffectType type) {
		this.type = type;
	}

	public static ArrayList<PotionEffect> calculateEffect(HashMap<String, Double> aspects){
		ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
		HashMap<PotionEffectType, PotionEffect> effectMap = new HashMap<PotionEffectType, PotionEffect>();
		//TODO: The gameplan:
		//Map <PotionEffectType, PotionEffect>
		//interate
		//Check map
		//if there's effectype already, put a new one based on name
		//if not, add new one
		//convert map to arraylist
		
		double bonusPotency = 0;
		double bonusDuration = 0;
		if(aspects.containsKey("RAW_POTENCY")) {
			bonusPotency = aspects.remove("RAW_POTENCY");
		}
		if(aspects.containsKey("RAW_DURATION")) {
			bonusPotency = aspects.remove("RAW_DURATION");
		}
		/*if(aspects.size() == 0 && aspects.containsKey(null)) {
			Brewery.breweryDriver.debugLog("Woah, empty?");
			return effects;
		}*/
		
		for(String currentAspect : aspects.keySet()) {
			Brewery.breweryDriver.debugLog("Processing - " + currentAspect);
			
			double potency = 1;
			double duration = 90;
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
				if(effectMap.containsKey(type)) {//already have
					if(isPotency) {//Assumes we don't have a potency aspect, but a duration
						potency = calculatePotency(aspects.get(currentAspect), bonusPotency, (PotionEffectType.getByName(trueAspect).isInstant()));
						effectMap.put(type, new PotionEffect(type, effectMap.get(type).getDuration(), (int)potency));
					} else { //We are a duration
						duration = calculateDuration(aspects.get(currentAspect), bonusDuration);
						effectMap.put(type, new PotionEffect(type, ((int) duration) * 20, effectMap.get(type).getAmplifier()));
					}
				} else {// we don't have it
					if(isPotency) {//Assumes we don't have a potency aspect, but a duration
						potency = calculatePotency(aspects.get(currentAspect), bonusPotency, (PotionEffectType.getByName(trueAspect).isInstant()));
						effectMap.put(type, new PotionEffect(type, ((int) duration) * 20, (int)potency));
					} else { //We are a duration
						duration = calculateDuration(aspects.get(currentAspect), bonusDuration);
						effectMap.put(type, new PotionEffect(type, ((int) duration) * 20, (int)potency));
					}
				}
				
			}
		}
		
		
		/*Iterator<Map.Entry<String, Double>> it = aspects.entrySet().iterator();
		while(it.hasNext()) {//TODO: be able to search for paired things w/ concurrentmodificationexception
			Map.Entry<String, Double> entry = it.next();
			String currentAspect = entry.getKey();
			double potency = 1;
			double duration = 90;
			String trueAspect = currentAspect;
			PotionEffectType type;
			//Parse the PotionEffect name
			if(currentAspect.contains("_POTENCY")) {
				trueAspect = currentAspect.substring(0, currentAspect.length() - 8);
			} else if (currentAspect.contains("_DURATION")) {
				trueAspect = currentAspect.substring(0, currentAspect.length() - 10);
			}
			//check if really potion type
			type = PotionEffectType.getByName(trueAspect);
			if(type != null){
				if(aspects.containsKey(trueAspect + "_POTENCY")) {
					potency = calculatePotency(aspects.remove(trueAspect + "_POTENCY"), bonusPotency, (PotionEffectType.getByName(trueAspect).isInstant()));
				}
				if(aspects.containsKey(trueAspect + "_DURATION")) {
				potency = calculateDuration(aspects.remove(trueAspect + "_DURATION"), bonusDuration);
				}
				effects.add(new PotionEffect(type, ((int) duration) * 20, (int)potency));
			}
		}*/
		
		for(PotionEffectType effectType: effectMap.keySet()) {
			effects.add(effectMap.get(effectType));
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
}
