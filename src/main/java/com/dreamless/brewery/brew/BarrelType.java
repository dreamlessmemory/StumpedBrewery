package com.dreamless.brewery.brew;

public enum BarrelType {
	OAK, DARK_OAK, BIRCH, ACACIA, JUNGLE, SPRUCE;
	
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
			return 1;
		case DARK_OAK:
			return 1;
		case BIRCH:
			return 1;
		case ACACIA:
			return 1;
		case JUNGLE:
			return 1;
		case SPRUCE:
			return 1;
		default:
			return 1;
		}
	}
	public int getDurationIncrease() {
		switch(this) {
		case OAK:
			return 1;
		case DARK_OAK:
			return 1;
		case BIRCH:
			return 1;
		case ACACIA:
			return 1;
		case JUNGLE:
			return 1;
		case SPRUCE:
			return 1;
		default:
			return 1;
		}
	}
}
