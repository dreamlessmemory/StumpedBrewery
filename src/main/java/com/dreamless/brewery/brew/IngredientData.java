package com.dreamless.brewery.brew;

import org.bukkit.Color;
import org.bukkit.potion.PotionEffectType;

public class IngredientData {
	
	private final String DRINK_NAME;
	private final String ALCOHOLIC_DRINK_NAME;
	private final PotionEffectType POTION_EFFECT;
	private final String FLAVOR_DESCRIPTOR;
	private final Rarity RARITY;
	private final Color COLOR;
	/**
	 * @param drinkName
	 * @param alcoholicDrinkName
	 * @param potionEffect
	 * @param flavorDescriptor
	 * @param rarity
	 */
	public IngredientData(String drinkName, String alcoholicDrinkName, PotionEffectType potionEffect,
			String flavorDescriptor, Rarity rarity, int color) {
		DRINK_NAME = drinkName;
		ALCOHOLIC_DRINK_NAME = alcoholicDrinkName;
		POTION_EFFECT = potionEffect;
		FLAVOR_DESCRIPTOR = flavorDescriptor;
		RARITY = rarity;
		COLOR = Color.fromRGB(color);
	}
	/**
	 * @return the drink name
	 */
	public String getDrinkName() {
		return DRINK_NAME;
	}
	/**
	 * @return the alcoholic drink name
	 */
	public String getAlcoholicDrinkName() {
		return ALCOHOLIC_DRINK_NAME;
	}
	/**
	 * @return the PotionEffect
	 */
	public PotionEffectType getPotionEffectType() {
		return POTION_EFFECT;
	}
	/**
	 * @return the flavor descriptor
	 */
	public String getFlavorDescriptor() {
		return FLAVOR_DESCRIPTOR;
	}
	/**
	 * @return the rarity
	 */
	public Rarity getRarity() {
		return RARITY;
	}
	
	public Color getColor()
	{
		return COLOR;
	}
	
	

}
