package com.dreamless.brewery.brew;

public enum BarrelType {
	OAK, DARK_OAK, BIRCH, ACACIA, JUNGLE, SPRUCE;
	
	private static final int TICKS_PER_MINUTE = 20 * 60; 
	
	public String toString() {
		switch(this) {
		case OAK:
			return "Oak";
		case DARK_OAK:
			return "Dark Oak";
		case BIRCH:
			return "Birch";
		case ACACIA:
			return "Acacia";
		case JUNGLE:
			return "Jungle";
		case SPRUCE:
			return "Spruce";
		default:
			return "Unknown";
		}
	}
	
	public int getLevelIncrease() {
		switch(this) {
		case ACACIA:
			return 0;
		case BIRCH:
			return 0;
		case OAK:
			return 50;
		case DARK_OAK:
			return 75;
		case SPRUCE:
			return 50;
		case JUNGLE:
			return 25;
		default:
			return 50;
		}
	}
	
	public double getAgingFactor() {
		switch(this) {
		case ACACIA:
			return 1.0;
		case BIRCH:
			return 10.0;
		case OAK:
			return 30.0;
		case DARK_OAK:
			return 30.0;
		case SPRUCE:
			return 10.0;
		case JUNGLE:
			return 1.0;
		default:
			return 30;
		}
	}
	
	public int getDurationIncrease() {
		switch(this) {
		case ACACIA:
			return 3;
		case BIRCH:
			return 25;
		case OAK:
			return 50;
		case DARK_OAK:
			return 30;
		case SPRUCE:
			return 10;
		case JUNGLE:
			return 1;
		default:
			return 30;
		}
	}
	
	public int getLevelCap() {
		switch(this) {
		case ACACIA:
			return 0;
		case BIRCH:
			return 0;
		case OAK:
			return 1;
		case DARK_OAK:
			return 2;
		case SPRUCE:
			return 3;
		case JUNGLE:
			return 4;
		default:
			return 1;
		}
	}
	
	public int getDurationCap() {
		switch(this) {
		case ACACIA:
			return 15 * TICKS_PER_MINUTE;
		case BIRCH:
			return 10 * TICKS_PER_MINUTE;
		case OAK:
			return 7 * TICKS_PER_MINUTE;
		case DARK_OAK:
			return 6 * TICKS_PER_MINUTE;
		case SPRUCE:
			return 5 * TICKS_PER_MINUTE;
		case JUNGLE:
			return 5 * TICKS_PER_MINUTE;
		default:
			return 6 * TICKS_PER_MINUTE;
		}
	}
	
}
