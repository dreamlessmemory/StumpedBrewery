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
	
	public void setValues(double potency, double saturation) {
		this.potency = potency;
		this.saturation = saturation;
	}
	
	public double getCookedBase() {
		return potency/Math.max(saturation, 1);
	}

	public String toString() {
		return "Potency: " + potency + " Saturation: " + saturation;
	}
	
	public static AspectRarity getRarityValues(int rarity) {
		switch(rarity) {
		case (1):
			return new AspectRarity(4, 0.2);
		case (2):
			return new AspectRarity(24, 0.6);
		case (3):
			return new AspectRarity(60, 1.0);
		default:
			return new AspectRarity(4, 0.2);
		}
	}

	public static double getFermentationIncrease(int time, String aspect, String type) {
		typeBonuses aspectBonuses = getTypeBonus(aspect, true);
		typeBonuses brewBonus = getTypeBonus(type, false);
		
		int inertia = aspectBonuses.getInertia() + brewBonus.getInertia();
		if(inertia > time) { //Not yet ready, so return zero
			return 0.0;
		}
		double multiplier = (double)brewBonus.getReactivity()/100 ;
		return aspectBonuses.getReactivity() * multiplier;		
	}
	
	//Returns a double multiplier
	public static double getEffectiveActivation(String aspect, double activation, String type) {
		typeBonuses aspectBonuses = getTypeBonus(aspect, true);
		typeBonuses brewBonus = getTypeBonus(type, false);
		
		int stability = aspectBonuses.getStability() + brewBonus.getStability();
		int saturation = aspectBonuses.getSaturation() + brewBonus.getSaturation();
		int integrity = aspectBonuses.getIntegrity() + brewBonus.getIntegrity();
		
		if(activation > stability) {//overdone
			double difference = activation - stability;
			difference *= (double)integrity/100;
			return Math.max(0, ((double) saturation - difference)/100);
		} else if (activation > saturation) { //Saturated but stable
			return (double) saturation/100;
		} else {//Undersaturated
			return (double)activation/100;
		}
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
	
	private static typeBonuses getTypeBonus(String type, boolean aspect) {
		String query = "SELECT * FROM " + Brewery.getDatabase(null) + (aspect? "aspects" : "typebonuses") + " WHERE " + (aspect? "aspect" : "type")+ "=?";
		try (PreparedStatement stmt = Brewery.connection.prepareStatement(query)) {
			stmt.setString(1, type);
			ResultSet results;
			results = stmt.executeQuery();
			if(!results.next()) {
				return new typeBonuses(0, 0, 0, 0, 0, 0);
			}
			
			int inertia = results.getInt("inertia");
			int reactivity = results.getInt("reactivity");
			int saturation = results.getInt("saturation");
			int stability = results.getInt("stability");
			int integrity = results.getInt("integrity");
			int distillation = (aspect? 100 :results.getInt("distillation"));
			
			return new typeBonuses(inertia, reactivity, saturation, stability, integrity, distillation);
			
		}  catch(SQLException e1) {
			e1.printStackTrace();
			return new typeBonuses(0, 0, 0, 0, 0, 0);
		}
	}
	
	public static double processFilter(String aspect, String type, double activation, Material filter) {
		
		typeBonuses aspectBonuses = getTypeBonus(aspect, true);
		typeBonuses brewBonus = getTypeBonus(type, false);
		
		double stability = (double)(aspectBonuses.getStability() + brewBonus.getStability())/100;
		double reactivity =  (double)(aspectBonuses.getReactivity() * brewBonus.getReactivity()) /100;
		
		switch(filter) {
			case GLOWSTONE_DUST:
				return glowstoneFilter(activation, reactivity, brewBonus.getDistillation());
			case REDSTONE:
				return redstoneFilter(activation, reactivity, brewBonus.getDistillation());
			case GUNPOWDER:
				return gunpowderFilter(activation, reactivity, brewBonus.getDistillation());
			case SUGAR:
				return sugarFilter(activation, reactivity, stability, brewBonus.getDistillation());
			default:
				return activation;
		}
	}
	//Increases everything under 100% to 100% by one half-step
	private static double glowstoneFilter(double activation, double reactivity, int multiplier) {
		if(activation < 1.0) {
			return Math.min(activation + (reactivity/((double)multiplier/100)/100) , 1.0);
		} else return activation;
	}
	//Decreases everything by one step
	private static double redstoneFilter(double activation, double reactivity, int multiplier) {
		return Math.max(activation - (reactivity * ((double)multiplier/100)/100) , 0);
	}
	//Increases everything above 100% by one half-step
	private static double gunpowderFilter(double activation, double reactivity, int multiplier) {
		if(activation >= 1.0) {
			return activation + (reactivity/((double)multiplier/100)/100);
		} else return activation;
	}
	//Decreases everything over stability by one half step
	private static double sugarFilter(double activation, double reactivity, double stability, int multiplier) {
		if(activation > stability) {
			return Math.max(activation - (reactivity/((double)multiplier/100)/100), stability);
		} else return activation;
	}
	
	private static class typeBonuses{
		private final int inertia;
		private final int reactivity;
		private final int saturation;
		private final int stability;
		private final int integrity;
		private final int distillation;
		
		public typeBonuses (int inertia, int reactivity, int saturation, int stability, int integrity, int distillation) {
			this.inertia = inertia;
			this.reactivity = reactivity;
			this.saturation = saturation;
			this.stability = stability;
			this.integrity = integrity;
			this.distillation = distillation;
		}

		public int getInertia() {
			return inertia;
		}

		public int getReactivity() {
			return reactivity;
		}

		public int getSaturation() {
			return saturation;
		}

		public int getStability() {
			return stability;
		}

		public int getIntegrity() {
			return integrity;
		}

		public int getDistillation() {
			return distillation;
		}
	}
	
	public static class AspectRarity{
		private final double potency;
		private final double saturation;
		public AspectRarity(double potency, double saturation) {
			this.potency = potency;
			this.saturation = saturation;
		}
		public double getSaturation() {
			return saturation;
		}
		public double getPotency() {
			return potency;
		}
	}
}
