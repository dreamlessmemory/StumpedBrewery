package com.dreamless.brewery.brew;

import java.util.Objects;

import org.bukkit.Material;

public class DrinkRecipe {
	
	private final Material primaryIngredient;
	private final Material secondaryIngredient;
	private final Material flavorIngredient;
	private final String barrelType;
	private final int alcoholLevel;
	private final String crafter;
	
	public DrinkRecipe(Material primary, Material secondary, Material flavor,  String barrelType, int alcoholLevel, String crafter)
	{
		primaryIngredient = primary;
		secondaryIngredient = secondary;
		flavorIngredient = flavor;
		this.barrelType = barrelType;
		this.alcoholLevel = alcoholLevel;
		this.crafter = crafter;
	}
	
	/**
	 * @return the primaryIngredient
	 */
	public Material getPrimaryIngredient() {
		return primaryIngredient;
	}

	/**
	 * @return the secondaryIngredient
	 */
	public Material getSecondaryIngredient() {
		return secondaryIngredient;
	}

	/**
	 * @return the flavorIngredient
	 */
	public Material getFlavorIngredient() {
		return flavorIngredient;
	}

	/**
	 * @return the barrelType
	 */
	public String getBarrelType() {
		return barrelType;
	}

	/**
	 * @return the crafter
	 */
	public String getCrafter() {
		return crafter;
	}

	/**
	 * @return the alcoholLevel
	 */
	public int getAlcoholLevel() {
		return alcoholLevel;
	}

	@Override
	public int hashCode() {
		return Objects.hash(alcoholLevel, barrelType, crafter, flavorIngredient, primaryIngredient,
				secondaryIngredient);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrinkRecipe other = (DrinkRecipe) obj;
		return alcoholLevel == other.alcoholLevel && barrelType == other.barrelType
				&& Objects.equals(crafter, other.crafter) && flavorIngredient == other.flavorIngredient
				&& primaryIngredient == other.primaryIngredient && secondaryIngredient == other.secondaryIngredient;
	}
	
	
	
}
