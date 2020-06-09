package com.dreamless.brewery.brew;

public enum BarrelType {
	OAK, DARK_OAK, BIRCH, ACACIA, JUNGLE, SPRUCE;
	
	// OAK - balanced
	// DARK_OAK - Balanced?
	// BIRCH - Duration > Level
	// ACACIA - Duration >> Level
	// JUNGLE - Level >> Duration
	// SPRUCE - Level > Duration
	
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
		case OAK:
			return 3;
		case DARK_OAK:
			return 3;
		case BIRCH:
			return 2;
		case ACACIA:
			return 1;
		case JUNGLE:
			return 7;
		case SPRUCE:
			return 5;
		default:
			return 1;
		}
	}
	public int getDurationIncrease() {
		switch(this) {
		case OAK:
			return 10;
		case DARK_OAK:
			return 10;
		case BIRCH:
			return 15;
		case ACACIA:
			return 20;
		case JUNGLE:
			return 5;
		case SPRUCE:
			return 7;
		default:
			return 1;
		}
	}
}
