package com.dreamless.brewery.brew;

public class AspectMinimalRequirement extends AspectRequirement{

	public AspectMinimalRequirement(Aspect aspect, int requirement) {
		super(aspect, requirement);
	}

	@Override
	public boolean checkRequirement(int level) {
		return level >= requirement;
	}

}
