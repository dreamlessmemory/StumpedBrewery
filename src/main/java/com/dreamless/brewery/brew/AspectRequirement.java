package com.dreamless.brewery.distillation;

import com.dreamless.brewery.fermentation.BreweryIngredient.Aspect;

public abstract class AspectRequirement {
	protected final Aspect aspect;
	protected final int requirement;
	
	public AspectRequirement(Aspect aspect, int requirement) {
		this.aspect = aspect;
		this.requirement = requirement;
	}
	
	public abstract boolean checkRequirement(int level); 
}
