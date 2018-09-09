package com.dreamless.brewery;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;

public class Ingredient {
	private Material type;
	private Map<String, String> aspects = new HashMap<String, String>();
	private int fermentationMultiplier = 1;
	private int agingMultiplier = 1;
	private int distillingMultiplier = 1;
	
	public Ingredient(Material type, Map<String, String> aspects, int fMult, int aMult, int dMult) {
		this.type = type;
		this.aspects = aspects;
		fermentationMultiplier = fMult;
		agingMultiplier = aMult;
		distillingMultiplier = dMult;
	}
	
	public Ingredient(Material type, Map<String, String> aspects) {
		this.type = type;
		this.aspects = aspects;
	}

	public Material getType() {
		return type;
	}
	
	public Map<String, String> getAspects() {
		return aspects;
	}

	public int getFermentationMultiplier() {
		return fermentationMultiplier;
	}

	public int getAgingMultiplier() {
		return agingMultiplier;
	}

	public int getDistillingMultiplier() {
		return distillingMultiplier;
	}
}
