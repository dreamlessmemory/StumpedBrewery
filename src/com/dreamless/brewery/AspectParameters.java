package com.dreamless.brewery;

import org.bukkit.configuration.ConfigurationSection;

public class AspectParameters {
	private double fermentationMultiplier;
	private int fermentationPeak;
	private int fermentationRange;
	private double agingMultiplier;
	private int agingPeak;
	private int agingRange;
	private double distillingMultiplier;
	private int distillingPeak;
	private int distillingRange;

	public AspectParameters(ConfigurationSection configSection) {
		fermentationMultiplier = configSection.getDouble("fermentationMultiplier", 0);
		fermentationPeak = configSection.getInt("fermentationPeak", 1);
		fermentationRange = configSection.getInt("fermentationRange", 1);
		agingMultiplier = configSection.getDouble("agingMultiplier", 0);
		agingPeak = configSection.getInt("agingPeak", 1);
		agingRange = configSection.getInt("agingRange", 1);
		distillingMultiplier = configSection.getDouble("distillingMultiplier", 0);
		distillingPeak = configSection.getInt("distillingPeak", 1);
		distillingRange = configSection.getInt("distillingRange", 1);
	}
	
	
	public double getFermentationStageStep(int time) {
		//int difference = time - fermentationPeak;
		if (time >= fermentationPeak - fermentationRange + 1 && time <= fermentationPeak + fermentationRange) {
			return fermentationMultiplier * (time - fermentationPeak <= 0 ? 1 : -1);
		}
		return 0.0;
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


	@Override
	public String toString() {
		return "AspectParameters [fermentationMultiplier=" + fermentationMultiplier + ", fermentationPeak="
				+ fermentationPeak + ", fermentationRange=" + fermentationRange + ", agingMultiplier=" + agingMultiplier
				+ ", agingPeak=" + agingPeak + ", agingRange=" + agingRange + ", distillingMultiplier="
				+ distillingMultiplier + ", distillingPeak=" + distillingPeak + ", distillingRange=" + distillingRange
				+ "]";
	}
	
}
