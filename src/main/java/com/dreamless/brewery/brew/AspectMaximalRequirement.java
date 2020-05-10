package com.dreamless.brewery.brew;

public class AspectMaximalRequirement extends AspectRequirement {

	public AspectMaximalRequirement(Aspect aspect, int requirement) {
		super(aspect, requirement);
	}

	@Override
	public boolean checkRequirement(int level) {
		return level <= requirement;
	}

}
