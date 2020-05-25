package com.dreamless.brewery.brew;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Color;
import org.bukkit.potion.PotionEffectType;

public enum BreweryEffect {

	ABSORPTION, DAMAGE_RESISTANCE, DOLPHINS_GRACE, FAST_DIGGING, FIRE_RESISTANCE, HEAL,	WATER_BREATHING, INCREASE_DAMAGE, INVISIBILITY, JUMP,	
	LEVITATION,	LUCK, NIGHT_VISION,	REGENERATION, SATURATION, SLOW_FALLING,	SPEED, NONE;
	
	private static final int DURATION_SCORE_MULTIPLIER = 20 * 1;
	private static final double DURATION_TO_LEVEL_SCALE = 0.3;
	private static final int LEVEL_THRESHOLD = 25;

	// Get the list of effects
	public static BreweryEffect getEffect(AspectMatrix matrix){	
		for(BreweryEffect effect : BreweryEffect.values()) {
			if(effect.getEffectRequirement().checkAspectRequirement(matrix)) {
				return effect;
			}
		}
		return NONE;
	}

	public PotionEffectType getPotionEffectType() {
		switch(this){
		case ABSORPTION:
			return PotionEffectType.ABSORPTION;
		case DAMAGE_RESISTANCE:
			return PotionEffectType.DAMAGE_RESISTANCE;
		case DOLPHINS_GRACE:
			return PotionEffectType.DOLPHINS_GRACE;
		case FAST_DIGGING:
			return PotionEffectType.FAST_DIGGING;
		case FIRE_RESISTANCE:	
			return PotionEffectType.FIRE_RESISTANCE;
		case HEAL:
			return PotionEffectType.HEAL;
		case WATER_BREATHING:	
			return PotionEffectType.WATER_BREATHING;
		case INCREASE_DAMAGE:	
			return PotionEffectType.INCREASE_DAMAGE;
		case INVISIBILITY:
			return PotionEffectType.INVISIBILITY;
		case JUMP:
			return PotionEffectType.JUMP;
		case LEVITATION:	
			return PotionEffectType.LEVITATION;
		case LUCK:
			return PotionEffectType.LUCK;
		case NIGHT_VISION:
			return PotionEffectType.NIGHT_VISION;
		case REGENERATION:	
			return PotionEffectType.REGENERATION;
		case SATURATION:
			return PotionEffectType.SATURATION;
		case SLOW_FALLING:	
			return PotionEffectType.SLOW_FALLING;
		case SPEED:
			return PotionEffectType.SPEED;
		default:
			return PotionEffectType.GLOWING;
		}
	}

	// TODO: Finish this
	public BreweryEffectRequirement getEffectRequirement() {
		BreweryEffectRequirement requirement;
		switch(this){
		case ABSORPTION:
		case DAMAGE_RESISTANCE:
		case DOLPHINS_GRACE:
		case FAST_DIGGING:
		case FIRE_RESISTANCE:
		case HEAL:
		case WATER_BREATHING:
		case INCREASE_DAMAGE:
		case INVISIBILITY:
		case JUMP:
		case LEVITATION:
		case LUCK:
		case NIGHT_VISION:
		case REGENERATION:
		case SATURATION:
		case SLOW_FALLING:
			requirement = new BreweryEffectRequirement(3);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.VOID, 1);
			break;
		case SPEED:
			requirement = new BreweryEffectRequirement(1);
			requirement.addAspectMaximalRequirement(Aspect.AERIAL, 2);
			break;
		default:
			requirement = new BreweryEffectRequirement(0);
		}
		return requirement;
	}

	public BreweryEffectAspectDistribution getEffectAspectDistribution() {
		BreweryEffectAspectDistribution distribution = new BreweryEffectAspectDistribution();
		switch(this){
		case ABSORPTION:
		case DAMAGE_RESISTANCE:
		case DOLPHINS_GRACE:
		case FAST_DIGGING:
		case FIRE_RESISTANCE:
		case HEAL:
		case WATER_BREATHING:
		case INCREASE_DAMAGE:
		case INVISIBILITY:
		case JUMP:
		case LEVITATION:
		case LUCK:
		case NIGHT_VISION:
		case REGENERATION:
		case SATURATION:
		case SLOW_FALLING:
			break;
		case SPEED:
			distribution.addMultipler(Aspect.AERIAL, 1.0);
			break;
		default:
			distribution.addMultipler(Aspect.AERIAL, 0);
		}
		return distribution;
	}

	//TODO: Finish this
	public Color getColor() {
		switch(this){
		case ABSORPTION:
		case DAMAGE_RESISTANCE:
		case DOLPHINS_GRACE:
		case FAST_DIGGING:
		case FIRE_RESISTANCE:
		case HEAL:
		case WATER_BREATHING:
		case INCREASE_DAMAGE:
		case INVISIBILITY:
		case JUMP:
		case LEVITATION:
		case LUCK:
		case NIGHT_VISION:
		case REGENERATION:
		case SATURATION:
		case SLOW_FALLING:
		case SPEED:
			return Color.WHITE;
		case NONE:
			return Color.BLACK;
		default:
			return Color.ORANGE;
		}
	}


	// Gets the score of each effect
	public int getEffectStrength(HashMap<Aspect, Integer> containedAspects){
		int total = 0;
		HashMap<Aspect, Double> distributionHashMap = this.getEffectAspectDistribution().multiplierMap;
		for(Entry<Aspect, Double> entry : distributionHashMap.entrySet()) {
			if(distributionHashMap.containsKey(entry.getKey())){
				total += containedAspects.get(entry.getKey()) * entry.getValue();	
			}
		}
		return total;
	}

	public int getEffectLevel(int potencyScore, int durationScore) {
		
		double finalScore = potencyScore + (getPotionEffectType().isInstant() ? durationScore * DURATION_TO_LEVEL_SCALE : 0);
		
		return (int) finalScore/LEVEL_THRESHOLD;
	}
	
	public int getEffectDuration(int levelScore, int durationScore) {
		return getPotionEffectType().isInstant() ? 0 : (int) (DURATION_SCORE_MULTIPLIER * durationScore);
	}
}
