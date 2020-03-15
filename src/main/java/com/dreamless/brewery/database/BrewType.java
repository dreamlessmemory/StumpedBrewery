package com.dreamless.brewery.database;

import java.util.ArrayDeque;

import com.dreamless.brewery.recipe.RecipeEnum.Aspect;

@Deprecated
public class BrewType {
	private final String name;
	private final ArrayDeque<Aspect> aspectPriority;
	private final String cookedName;
	private final String distilledName;
	private final int colour;
	private final byte alcohol;
	
	public BrewType(String name, ArrayDeque<Aspect> aspectPriority, String cookedName, String distilledName, int colour, byte alcohol) {
		this.name = name;
		this.aspectPriority = aspectPriority;
		this.cookedName = cookedName;
		this.distilledName = distilledName;
		this.colour = colour;
		this.alcohol = alcohol;
	}

	public final String getName() {
		return name;
	}

	public final ArrayDeque<Aspect> getAspectPriority() {
		return aspectPriority;
	}

	public final String getCookedName() {
		return cookedName;
	}

	public final String getDistilledName() {
		return distilledName;
	}

	public final byte getAlcohol() {
		return alcohol;
	}

	public int getColour() {
		return colour;
	}
}
