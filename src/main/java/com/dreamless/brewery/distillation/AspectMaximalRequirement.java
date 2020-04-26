package com.dreamless.brewery.distillation;

import com.dreamless.brewery.fermentation.BreweryIngredient.Aspect;

public class AspectMaximalRequirement extends AspectRequirement {

	public AspectMaximalRequirement(Aspect aspect, int requirement) {
		super(aspect, requirement);
	}

	@Override
	public boolean checkRequirement(int level) {
		return level <= requirement;
	}

}
