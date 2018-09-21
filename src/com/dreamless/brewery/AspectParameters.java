package com.dreamless.brewery;

import org.bukkit.configuration.ConfigurationSection;

public class AspectParameters {
	private double fermentationMultiplier;
	private int [] fermentationCurve;
	private double agingMultiplier;
	private int [] agingCurve;
	private double distillingMultiplier;
	private int distillingCurve[];

	public AspectParameters(ConfigurationSection configSection) {
		fermentationMultiplier = configSection.getDouble("fermentationMultiplier", 0);
		agingMultiplier = configSection.getDouble("agingMultiplier", 0);
		distillingMultiplier = configSection.getDouble("distillingMultiplier", 0);
		
		//Parse Curves
		if(fermentationMultiplier != 0) {
			String[] numbers = configSection.getString("fermentationCurve", "").split("/");
			if(numbers.length > 1) {
				fermentationCurve = new int[numbers.length];
				for(int i = 0; i < numbers.length; i++) {
					fermentationCurve[i] = Integer.parseInt(numbers[i]);
				}
			}
		}
		
		if(agingMultiplier != 0) {
			String[] numbers = configSection.getString("agingCurve", "").split("/");
			if(numbers.length > 1) {
				agingCurve = new int[numbers.length];
				for(int i = 0; i < numbers.length; i++) {
					agingCurve[i] = Integer.parseInt(numbers[i]);
				}
			}
		}
		
		if(fermentationMultiplier != 0) {
			String[] numbers = configSection.getString("distillingCurve", "").split("/");
			if(numbers.length > 1){
				distillingCurve = new int[numbers.length];
				for(int i = 0; i < numbers.length; i++) {
					distillingCurve[i] = Integer.parseInt(numbers[i]);
				}
			}
		}
	}
	
	
	public double getFermentationStageStep(int time) {
		if(fermentationCurve.length == 2) { //ramp up only
			if (time >= fermentationCurve[0] && time <= fermentationCurve[1]) {
				return fermentationMultiplier;
			}
			//return  ((time >= fermentationCurve[0] && time <= fermentationCurve[1]) <= 0 ? 1 : -1);
		} else if (fermentationCurve.length == 3) {
			if (time >= fermentationCurve[0] && time <= fermentationCurve[1]) {
				return fermentationMultiplier;
			} else if (time >= fermentationCurve[2]) {
				return fermentationMultiplier * -1;
			}
		} else if (fermentationCurve.length == 4) {
			if (time >= fermentationCurve[0] && time <= fermentationCurve[1]) {
				return fermentationMultiplier;
			} else if (time >= fermentationCurve[2] && time <= fermentationCurve[3]) {
				return fermentationMultiplier * -1;
			} else return 0.0;
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
		return "AspectParameters [fermentationMultiplier=" + fermentationMultiplier + ", agingMultiplier=" + agingMultiplier + ", distillingMultiplier="
				+ distillingMultiplier + "]";
	}
	
}
