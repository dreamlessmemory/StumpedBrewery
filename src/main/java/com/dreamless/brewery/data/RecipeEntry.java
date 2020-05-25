package com.dreamless.brewery.data;

import com.dreamless.brewery.brew.BreweryEffect;

public class RecipeEntry {
	
	private static final String STRING_SEPARATOR = "-";

	private final BreweryEffect effect;
	private final int potencyScore;
	private final int durationScore;
	
	public RecipeEntry(BreweryEffect effect, int potency, int duration) {
		this.effect = effect;
		this.potencyScore = potency;
		this.durationScore = duration;
	}
	
	public String generateKey() {
		return effect.toString() + STRING_SEPARATOR + potencyScore + STRING_SEPARATOR + durationScore;
	}
	
	public BreweryEffect getEffect() {
		return effect;
	}

	public int getPotencyScore() {
		return potencyScore;
	}

	public int getDurationScore() {
		return durationScore;
	}
	
	@Override
	public int hashCode() {
		return generateKey().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
		
        RecipeEntry other = (RecipeEntry) obj;
        
		return other.effect.equals(this.effect) &&
				other.potencyScore == this.potencyScore &&
				other.durationScore == this.durationScore;
	}
	
}
