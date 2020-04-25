package com.dreamless.brewery.recipe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.w3c.dom.ranges.RangeException;

import com.dreamless.brewery.recipe.BreweryIngredient.Aspect;
import com.google.common.collect.Range;
import com.google.gson.Gson;

public class BreweryEffect {

	public enum PotionEffect{
		ABSORPTION, DAMAGE_RESISTANCE, DOLPHINS_GRACE, FAST_DIGGING, FIRE_RESISTANCE, HEAL,	HEALTH_BOOST, INCREASE_DAMAGE, INVISIBILITY, JUMP,	
		LEVITATION,	LUCK, NIGHT_VISION,	REGENERATION, SATURATION, SLOW_FALLING,	SPEED;

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
			case HEALTH_BOOST:	
				return PotionEffectType.HEALTH_BOOST;
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

		//TODO Finalize this
		public int getCost() {
			switch(this){
			case FAST_DIGGING:
			case LUCK:
				return 3;
			case SPEED:
			case ABSORPTION:
			case DAMAGE_RESISTANCE:
			case DOLPHINS_GRACE:
			case FIRE_RESISTANCE:
			case HEAL:
			case HEALTH_BOOST:
			case INCREASE_DAMAGE:
				return 2;
			case INVISIBILITY:
			case JUMP:
			case LEVITATION:
			case NIGHT_VISION:
			case REGENERATION:
			case SATURATION:
			case SLOW_FALLING:
			default:
				return 1;
			}
		}
		
		public BreweryEffectRequirement getEffectRequirement() {
			BreweryEffectRequirement requirement;
			switch(this){
			case ABSORPTION:
			case DAMAGE_RESISTANCE:
			case DOLPHINS_GRACE:
			case FAST_DIGGING:
			case FIRE_RESISTANCE:
			case HEAL:
			case HEALTH_BOOST:
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
			default:
				requirement = new BreweryEffectRequirement(0);
			}
			return requirement;
		}
	}
	
	// TODO: Implementation
	public static Set<PotionEffectType> getPotionEffectTypes(BreweryAspectMatrix matrix){
		HashSet<PotionEffectType> set = new HashSet<PotionEffectType>();
		return set;
	}
	
	
	
	public class BreweryAspectMatrix{
		private HashMap<Aspect, Integer> aspectMatrix;
		private int totalCount = 18;
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
		public void distillAspect(Aspect aspect) {
			aspectMatrix.put(aspect, Math.max(aspectMatrix.get(aspect) - 1, 0));
		}
	}

	public static class BreweryEffectRequirement{
		public final int totalMax;
		public HashMap<Aspect, Range<Integer>> aspectRequriementMap;
		public BreweryEffectRequirement(int max) {
			totalMax = max;
		}
		public void addAspectRequirement(Aspect aspect, int min, int max) {
			aspectRequriementMap.put(aspect, Range.closed(min, max));
		}
	}
	
}
