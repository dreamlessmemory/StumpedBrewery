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
	
	public double getAgingRequirement() {
		switch(this) {
		case ACACIA:
			return 100;
		case BIRCH:
			return 10;
		case OAK:
			return 1;
		case DARK_OAK:
			return 10;
		case SPRUCE:
			return 10;
		case JUNGLE:
			return 100;
		default:
			return 1;
		}
	}
	
	public int getLevelCap() {
		switch(this) {
		case ACACIA:
			return 0;
		case BIRCH:
			return 1;
		case OAK:
			return 0;
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
			return 10 * TICKS_PER_MINUTE;
		case BIRCH:
			return 8 * TICKS_PER_MINUTE;
		case OAK:
			return (int) (0.5 * TICKS_PER_MINUTE);
		case DARK_OAK:
			return 4 * TICKS_PER_MINUTE;
		case SPRUCE:
			return 2 * TICKS_PER_MINUTE;
		case JUNGLE:
			return 2 * TICKS_PER_MINUTE;
		default:
			return (int) (0.5 * TICKS_PER_MINUTE);
		}
	}
	
}
