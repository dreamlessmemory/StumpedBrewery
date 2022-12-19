package com.dreamless.brewery.brew;

public enum Rarity {
	COMMON, RARE;
	
	public final int getEffectPotency()
	{
		switch(this) {
		case COMMON:
			return 75;
		case RARE:
			return 100;
		default:
			return 75;
		}
	}
	
}
