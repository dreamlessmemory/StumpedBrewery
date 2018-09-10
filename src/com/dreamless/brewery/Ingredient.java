package com.dreamless.brewery;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;

public class Ingredient {
	private Material type;
	private Map<String, String> aspects = new HashMap<String, String>();
	private double fermentationMultiplier = 1;
	private double agingMultiplier = 1;
	private double distillingMultiplier = 1;
	
	public Ingredient(Material type, Map<String, String> aspects, double fMult, double aMult, double dMult) {
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

	public double getFermentationMultiplier() {
		return fermentationMultiplier;
	}

	public double getAgingMultiplier() {
		return agingMultiplier;
	}

	public double getDistillingMultiplier() {
		return distillingMultiplier;
	}
}
