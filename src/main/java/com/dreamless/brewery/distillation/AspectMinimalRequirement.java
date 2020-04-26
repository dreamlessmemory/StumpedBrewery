package com.dreamless.brewery.distillation;

import com.dreamless.brewery.fermentation.BreweryIngredient.Aspect;

public class AspectMinimalRequirement extends AspectRequirement{

	public AspectMinimalRequirement(Aspect aspect, int requirement) {
		super(aspect, requirement);
	}

	@Override
	public boolean checkRequirement(int level) {
		return level >= requirement;
	}

}
