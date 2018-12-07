package com.dreamless.brewery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Material;

public class Aspect implements Comparable<Object> {
	//Static Numbers for balancing
	public static double commonPotency = 4;
	public static double rarePotency = 24;
	public static double legendaryPotency = 60;
	public static double commonSaturation = 0.2;
	public static double rareSaturation = 0.6;
	public static double legendarySaturation = 1.0;
	
	//Private
	private double potency = 0;
	private double saturation = 0.0;
	private double activation = 0.0;
	
	public Aspect (double potency, double saturation) {
		this.potency = potency;
		this.saturation = saturation;
	}

	public double getActivation() {
		return activation;
	}

	public void setActivation(double activation) {
		this.activation = activation;
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
				return 4;
			case (2):
				return 24;
			case (3):
				return 60;
			default:
				return 4;
		}
	}
	
	public static double calculateRaritySaturation(int rarity){
		switch(rarity) {
		case (1):
			return 0.2;
		case (2):
			return 0.6;
		case (3):
			return 1.0;
		default:
			return 0.2;
		}
	}

	public static double getFermentationIncrease(int time, String aspect, String type) {
		String query = "SELECT reactivity, inertia FROM aspects WHERE aspect=?";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setString(1, aspect);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("There's no Aspect data. Please add for " + aspect);
				return 0;
			} else {//Successful Pull
				
				int inertia = getInertia(results.getInt("inertia"), type);
				if(inertia > time) { //Not yet ready, so return zero
					return 0.0;
				}
				double multiplier = getReactivityMultiplier(type);
				return results.getInt("reactivity") * multiplier;
				
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return 0.0;
	}
	
	//Returns a double multiplier
	public static double getEffectiveActivation(String aspect, double activation, String type) {
		String query = "SELECT stability, saturation, integrity FROM aspects WHERE aspect=?";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setString(1, aspect);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("There's no Aspect data. Please add for " + aspect);
				return 0;
			} else {//Successful Pull
				int stability = getStability(results.getInt("stability"), type);
				int saturation = getSaturation(results.getInt("saturation"), type);
				int integrity = getIntegrity(results.getInt("integrity"), type);
				
				if(activation > stability) {//overdone
					double difference = activation - stability;
					difference *= (double)integrity/100;
					return Math.max(0, ((double) saturation - difference)/100);
				} else if (activation > saturation) {
					return (double) saturation/100;
				} else {
					return activation/100;
				}					
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		return 0.0;
	}
	
	
	@Override
	public int compareTo(Object arg0) {
		double difference = this.potency - ((Aspect)arg0).getPotency();
		if(difference > 0) {
			return 1; 
		} else if (difference < 0) {
			return -1;
		} else {
			return 0;
		}
	}
	
	private static int getInertia(int inertia, String type) {
		switch(type) {
			case "CHOCOLATE":
				inertia = Math.max(0, inertia - 3);
				break;
			case "CIDER":
				inertia = Math.max(0,  inertia -2);
				break;
		}
		return inertia;
	}
	
	private static double getReactivityMultiplier(String type) {
		switch(type) {
			case "NETHER":
				return 1.5;
			case "WINE":
			case "CIDER":
				return 1.25;
			case "END":
				return 0.75;
			default:
				return 1.0;
		}
	}
	
	private static int getStability(int stability, String type) {
		switch(type) {
			case "WINE":
				return stability + 50;
			case "BEER":
			case "RUM":
				return stability + 25;
			default:
				return stability;
		}
	}
	
	private static int getIntegrity(int integrity, String type) {
		switch(type) {
			case "NETHER":
				return Math.min(75, integrity + 10);
			case "TEA":
				return Math.max(0, integrity - 35);
			default:
				return integrity;
		}
	}
	
	private static int getSaturation(int saturation, String type) {
		switch(type) {
			case "TEA":
				return saturation + 25;
			default:
				return saturation;
		}
	}
	
	public static double processFilter(String aspect, String type, double activation, Material filter) {
		String query = "SELECT stability, saturation, reactivity FROM aspects WHERE aspect=?";
		
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setString(1, aspect);
			ResultSet results;
			results = stmt.executeQuery();
			if (!results.next()) {
				Brewery.breweryDriver.debugLog("There's no Aspect data. Please add for " + aspect);
				return 0;
			} else {//Successful Pull
				double stability = getStability(results.getInt("stability"), type) / 100;
				double saturation = getSaturation(results.getInt("saturation"), type) / 100;
				double reactivity = results.getInt("reactivity") * getReactivityMultiplier(type) /100;
				
				switch(filter) {
					case GLOWSTONE_DUST:
						return glowstoneFilter(activation, reactivity);
					default:
						return activation;
				}
							
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			return activation;
		}
	}
	
	private static double glowstoneFilter(double activation, double reactivity) {
		if(activation < 1.0) {
			return Math.min(activation + reactivity/2 , 1.0);
		} else return activation;
	}
	
	//Glowstone
	//Redstone
	//Quartz
	//Lapis
	
	
}
