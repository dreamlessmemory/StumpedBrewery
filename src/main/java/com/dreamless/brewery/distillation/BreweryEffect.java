package com.dreamless.brewery.distillation;

import java.util.HashMap;
import org.bukkit.potion.PotionEffectType;

import com.dreamless.brewery.fermentation.BreweryIngredient.Aspect;
import com.dreamless.brewery.distillation.*;
import com.google.common.collect.Range;

public class BreweryEffect {
	
	public enum PotionEffect{
		ABSORPTION, DAMAGE_RESISTANCE, DOLPHINS_GRACE, FAST_DIGGING, FIRE_RESISTANCE, HEAL,	WATER_BREATHING, INCREASE_DAMAGE, INVISIBILITY, JUMP,	
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

		//TODO Finalize this
		public int getCost() {
			switch(this){
			case FAST_DIGGING:
			case LUCK:
			case ABSORPTION:
				return 3;
			case SPEED:
			case DAMAGE_RESISTANCE:
			case DOLPHINS_GRACE:
			case FIRE_RESISTANCE:
			case HEAL:
			case INCREASE_DAMAGE:
			case REGENERATION:
			case WATER_BREATHING:
				return 2;
			case INVISIBILITY:
			case JUMP:
			case LEVITATION:
			case NIGHT_VISION:
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
				requirement = new BreweryEffectRequirement(0);
				break;
			default:
				requirement = new BreweryEffectRequirement(0);
			}
			return requirement;
		}
	}	
	
}
