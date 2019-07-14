package com.dreamless.brewery.database;

import java.util.ArrayDeque;

import com.dreamless.brewery.recipe.Aspect;

public class BrewType {
	private final String name;
	private final ArrayDeque<Aspect> aspectPriority;
	private final String cookedName;
	private final String distilledName;
	private final byte alcoholMin;
	private final byte alcoholMax;
	private final byte alcoholStep;
	
	public BrewType(String name, ArrayDeque<Aspect> aspectPriority, String cookedName, String distilledName, byte alcoholMin, byte alcoholMax, byte alcoholStep) {
		this.name = name;
		this.aspectPriority = aspectPriority;
		this.cookedName = cookedName;
		this.distilledName = distilledName;
		this.alcoholMin = alcoholMin;
		this.alcoholMax = alcoholMax;
		this.alcoholStep = alcoholStep;
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

	public final byte getAlcoholMin() {
		return alcoholMin;
	}

	public final byte getAlcoholMax() {
		return alcoholMax;
	}

	public final byte getAlcoholStep() {
		return alcoholStep;
	}
}
