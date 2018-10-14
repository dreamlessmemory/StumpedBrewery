package com.dreamless.brewery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.collect.Range;

public class Aspect implements Comparable<Object> {
	//Static Numbers for balancing
	public static double commonPotency = 6;
	public static double uncommonPotency = 20;
	public static double superiorPotency = 42;
	public static double rarePotency = 64;
	public static double legendaryPotency = 100;
	public static double commonSaturation = 0.2;
	public static double uncommonSaturation = 0.4;
	public static double superiorSaturation = 0.6;
	public static double rareSaturation = 0.8;
	public static double legendarySaturation = 1.0;
	
	//Private
	private double potency = 0;
	private double saturation = 0.0;
	
	public Aspect (double potency, double saturation) {
		this.potency = potency;
		this.saturation = saturation;
	}

	public double getPotency() {
		return potency;
	}

	public void setPotency(double potency) {
		this.potency = potency;
	}

	public double getSaturation() {
		return saturation;
	}

	public void setSaturation(double saturation) {
		this.saturation = saturation;
	}

	public String toString() {
		return "Potency: " + potency + " Saturation: " + saturation;
	}
	
	public static double calculateRarityPotency(int rarity){
		switch(rarity) {
			case (1):
				return 6;
			case (2):
				return 20;
			case (3):
				return 42;
			case (4):
				return 64;
			case (5):
				return 100;
			default:
				return 6;
		}
	}
	
	public static double calculateRaritySaturation(int rarity){
		switch(rarity) {
		case (1):
			return 0.2;
		case (2):
			return 0.4;
		case (3):
			return 0.6;
		case (4):
			return 0.8;
		case (5):
			return 1.0;
		default:
			return 0.2;
		}
	}

	public static double getStepBonus(int time, String aspect, String stage) {
		String query = "SELECT * FROM " + stage + " WHERE aspect=?";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setString(1, aspect);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("There's no Aspect data. Please add for " + stage + " - " + aspect);
				return 0;
			} else {//Successful Pull
				//get paramenters
				int rampUpStart = results.getInt("rampupstart");
				int rampUpEnd = results.getInt("rampupend");
				int falloffStart = results.getInt("falloffstart");
				int falloffEnd = results.getInt("falloffend");
				double multiplier = results.getDouble("multiplier");
				//check if in rampup
				if(rampUpStart != 0 && rampUpEnd != 0 && Range.closed(rampUpStart, rampUpEnd).contains(time)) {
					return multiplier;
				} else if (falloffStart != 0 && falloffEnd !=0 && Range.closed(falloffStart, falloffEnd).contains(time)) {
					return multiplier * -1;
				} else {
					return 0;
				}
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return 0.0;
	}
	
	
	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		double difference = this.potency - ((Aspect)arg0).getPotency();
		if(difference > 0) {
			return 1; 
		} else if (difference < 0) {
			return -1;
		} else {
			return 0;
		}
	}
}
