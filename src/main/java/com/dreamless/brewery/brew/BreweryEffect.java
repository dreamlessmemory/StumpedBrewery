package com.dreamless.brewery.brew;

import org.bukkit.potion.PotionEffectType;

public class BreweryEffect {
	
	private static final int DURATION_SCORE_MULTIPLIER = 20 * 3;
	private static final double DURATION_TO_LEVEL_SCALE = 0.15;
	private static final int LEVEL_THRESHOLD = 50;
	
	public static int calculateEffectLevel(PotionEffectType effect, int potencyScore, int durationScore, BarrelType type) {
		
		double finalScore = potencyScore + (effect.isInstant() ? durationScore * DURATION_TO_LEVEL_SCALE : 0);
		
		return Math.min(type.getLevelCap(), (int)finalScore/LEVEL_THRESHOLD);
	}
	
	public static int calcuateEffectDuration(PotionEffectType effect, int score, BarrelType type) {
		return effect.isInstant() ? 0 : Math.min(type.getDurationCap(), (int) (DURATION_SCORE_MULTIPLIER * score));
	}
}
