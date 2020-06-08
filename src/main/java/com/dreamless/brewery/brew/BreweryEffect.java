package com.dreamless.brewery.brew;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Color;
import org.bukkit.potion.PotionEffectType;

public enum BreweryEffect {

	ABSORPTION, DAMAGE_RESISTANCE, DOLPHINS_GRACE, FAST_DIGGING, FIRE_RESISTANCE, HEAL,	WATER_BREATHING, INCREASE_DAMAGE, INVISIBILITY, JUMP,	
	LEVITATION,	LUCK, NIGHT_VISION,	REGENERATION, SATURATION, SLOW_FALLING,	SPEED, NONE;
	
	private static final int DURATION_SCORE_MULTIPLIER = 20 * 5;
	private static final double DURATION_TO_LEVEL_SCALE = 0.3;
	private static final int LEVEL_THRESHOLD = 15;

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

	public BreweryEffectRequirement getEffectRequirement() {
		BreweryEffectRequirement requirement;
		switch(this){
		case ABSORPTION:
			requirement = new BreweryEffectRequirement(7);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 3);
			requirement.addAspectMaximalRequirement(Aspect.AERIAL, 1);
			requirement.addAspectMaximalRequirement(Aspect.VOID, 1);
			requirement.addAspectMinimalRequirement(Aspect.AQUATIC, 3);
			break;
		case DAMAGE_RESISTANCE:
			requirement = new BreweryEffectRequirement(6);
			requirement.addAspectMinimalRequirement(Aspect.LITHIC, 3);
			requirement.addAspectMinimalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMaximalRequirement(Aspect.AERIAL, 0);
			requirement.addAspectMaximalRequirement(Aspect.AQUATIC, 0);
			break;
		case DOLPHINS_GRACE:
			requirement = new BreweryEffectRequirement(4);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 1);
			requirement.addAspectMaximalRequirement(Aspect.PYROTIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.VOID, 2);
			requirement.addAspectMinimalRequirement(Aspect.AQUATIC, 2);
			break;
		case FAST_DIGGING:
			requirement = new BreweryEffectRequirement(8);
			requirement.addAspectMinimalRequirement(Aspect.LITHIC, 3);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 1);
			requirement.addAspectMaximalRequirement(Aspect.PYROTIC, 1);
			requirement.addAspectMinimalRequirement(Aspect.AERIAL, 3);
			requirement.addAspectMaximalRequirement(Aspect.VOID, 1);
			requirement.addAspectMaximalRequirement(Aspect.AQUATIC, 1);
			break;
		case FIRE_RESISTANCE:
			requirement = new BreweryEffectRequirement(6);
			requirement.addAspectMinimalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 3);
			requirement.addAspectMaximalRequirement(Aspect.VOID, 0);
			requirement.addAspectMaximalRequirement(Aspect.AQUATIC, 0);
			break;
		case HEAL:
			requirement = new BreweryEffectRequirement(5);
			requirement.addAspectMinimalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMaximalRequirement(Aspect.PYROTIC, 1);
			requirement.addAspectMaximalRequirement(Aspect.AERIAL, 2);
			requirement.addAspectMaximalRequirement(Aspect.VOID, 2);
			requirement.addAspectMinimalRequirement(Aspect.AQUATIC, 2);
			break;
		case WATER_BREATHING:
			requirement = new BreweryEffectRequirement(5);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 1);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 1);
			requirement.addAspectMaximalRequirement(Aspect.PYROTIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.AERIAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.AQUATIC, 2);
			break;
		case INCREASE_DAMAGE:
			requirement = new BreweryEffectRequirement(4);
			requirement.addAspectMinimalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.AERIAL, 2);
			requirement.addAspectMaximalRequirement(Aspect.VOID, 2);
			requirement.addAspectMaximalRequirement(Aspect.AQUATIC, 1);
			break;
		case INVISIBILITY:
			requirement = new BreweryEffectRequirement(3);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.INFERNAL, 1);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 1);
			requirement.addAspectMinimalRequirement(Aspect.AERIAL, 1);
			requirement.addAspectMaximalRequirement(Aspect.VOID, 2);
			requirement.addAspectMaximalRequirement(Aspect.AQUATIC, 2);
			break;
		case JUMP:
			requirement = new BreweryEffectRequirement(2);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMaximalRequirement(Aspect.PYROTIC, 1);
			requirement.addAspectMinimalRequirement(Aspect.AERIAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.VOID, 1);
			break;
		case SLOW_FALLING:
			requirement = new BreweryEffectRequirement(2);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.VOID, 1);
			break;
		case LUCK:
			requirement = new BreweryEffectRequirement(7);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.INFERNAL, 3);
			requirement.addAspectMaximalRequirement(Aspect.PYROTIC, 1);
			requirement.addAspectMaximalRequirement(Aspect.AERIAL, 0);
			requirement.addAspectMinimalRequirement(Aspect.VOID, 3);
			requirement.addAspectMaximalRequirement(Aspect.AQUATIC, 2);
			break;
		case NIGHT_VISION:
			requirement = new BreweryEffectRequirement(2);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 2);
			break;
		case REGENERATION:
			requirement = new BreweryEffectRequirement(6);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.LITHIC, 1);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMaximalRequirement(Aspect.PYROTIC, 1);
			requirement.addAspectMaximalRequirement(Aspect.AERIAL, 2);
			requirement.addAspectMaximalRequirement(Aspect.VOID, 2);
			requirement.addAspectMinimalRequirement(Aspect.AQUATIC, 3);
			break;
		case SATURATION:
			requirement = new BreweryEffectRequirement(0);
			break;
		case LEVITATION:
			requirement = new BreweryEffectRequirement(2);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 2);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 2);
			requirement.addAspectMinimalRequirement(Aspect.PYROTIC, 2);
			requirement.addAspectMinimalRequirement(Aspect.VOID, 1);
			requirement.addAspectMaximalRequirement(Aspect.AQUATIC, 2);
			break;
		case SPEED:
			requirement = new BreweryEffectRequirement(5);
			requirement.addAspectMaximalRequirement(Aspect.LITHIC, 0);
			requirement.addAspectMaximalRequirement(Aspect.INFERNAL, 1);
			requirement.addAspectMinimalRequirement(Aspect.AERIAL, 3);
			requirement.addAspectMinimalRequirement(Aspect.VOID, 2);
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
			distribution.addMultipler(Aspect.PYROTIC, 0.5);
			distribution.addMultipler(Aspect.AQUATIC, 0.5);
			break;
		case DAMAGE_RESISTANCE:
			distribution.addMultipler(Aspect.LITHIC, 0.6);
			distribution.addMultipler(Aspect.INFERNAL, 0.4);
			break;
		case DOLPHINS_GRACE:
			distribution.addMultipler(Aspect.VOID, 0.6);
			distribution.addMultipler(Aspect.AQUATIC, 0.4);
			break;
		case FAST_DIGGING:
			distribution.addMultipler(Aspect.LITHIC, 0.3);
			distribution.addMultipler(Aspect.VOID, 0.35);
			distribution.addMultipler(Aspect.AERIAL, 0.35);
			break;
		case FIRE_RESISTANCE:
			distribution.addMultipler(Aspect.PYROTIC, 0.3);
			distribution.addMultipler(Aspect.INFERNAL, 0.35);
			distribution.addMultipler(Aspect.AQUATIC, 0.35);
			break;
		case HEAL:
			distribution.addMultipler(Aspect.LITHIC, 0.5);
			distribution.addMultipler(Aspect.AQUATIC, 0.5);
			break;
		case WATER_BREATHING:
			distribution.addMultipler(Aspect.AERIAL, 0.5);
			distribution.addMultipler(Aspect.VOID, 0.5);
			break;
		case INCREASE_DAMAGE:
			distribution.addMultipler(Aspect.LITHIC, 0.3);
			distribution.addMultipler(Aspect.INFERNAL, 0.35);
			distribution.addMultipler(Aspect.PYROTIC, 0.35);
			break;
		case INVISIBILITY:
			distribution.addMultipler(Aspect.INFERNAL, 0.3);
			distribution.addMultipler(Aspect.VOID, 0.35);
			distribution.addMultipler(Aspect.AERIAL, 0.35);
			break;
		case JUMP:
			distribution.addMultipler(Aspect.AERIAL, 0.75);
			distribution.addMultipler(Aspect.VOID, 0.25);
			break;
		case LEVITATION:
			distribution.addMultipler(Aspect.AERIAL, 0.25);
			distribution.addMultipler(Aspect.VOID, 0.75);
			break;
		case LUCK:
			distribution.addMultipler(Aspect.PYROTIC, 0.15);
			distribution.addMultipler(Aspect.AQUATIC, 0.15);
			distribution.addMultipler(Aspect.VOID, 0.35);
			distribution.addMultipler(Aspect.INFERNAL, 0.35);
			break;
		case NIGHT_VISION:
			distribution.addMultipler(Aspect.VOID, 1.0);
			break;
		case REGENERATION:
			distribution.addMultipler(Aspect.AQUATIC, 0.65);
			distribution.addMultipler(Aspect.LITHIC, 0.35);
			break;
		case SATURATION:
			distribution.addMultipler(Aspect.AERIAL, 0.25);
			distribution.addMultipler(Aspect.LITHIC, 0.25);
			distribution.addMultipler(Aspect.PYROTIC, 0.25);
			distribution.addMultipler(Aspect.VOID, 0.25);
			distribution.addMultipler(Aspect.INFERNAL, 0.25);
			distribution.addMultipler(Aspect.AQUATIC, 0.25);
			break;
		case SLOW_FALLING:
			distribution.addMultipler(Aspect.AERIAL, 0.5);
			distribution.addMultipler(Aspect.AERIAL, 0.5);
			break;
		case SPEED:
			distribution.addMultipler(Aspect.AERIAL, 0.5);
			distribution.addMultipler(Aspect.VOID, 0.5);
			distribution.addMultipler(Aspect.PYROTIC, 0.5);
			break;
		default:
			distribution.addMultipler(Aspect.AERIAL, 0);
		}
		return distribution;
	}

	public Color getColor() {
		switch(this){
		case ABSORPTION:
		case REGENERATION:
		case FIRE_RESISTANCE:
		case HEAL:
		case INCREASE_DAMAGE:
			return Color.RED;
		case DAMAGE_RESISTANCE:
		case DOLPHINS_GRACE:
		case WATER_BREATHING:
			return Color.BLUE;
		case LUCK:
			return Color.LIME;
		case SATURATION:
		case FAST_DIGGING:
			return Color.ORANGE;
		case NIGHT_VISION:
		case SLOW_FALLING:
		case SPEED:
		case INVISIBILITY:
		case JUMP:
		case LEVITATION:
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
