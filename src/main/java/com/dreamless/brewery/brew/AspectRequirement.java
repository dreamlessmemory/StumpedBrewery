package com.dreamless.brewery.brew;

public abstract class AspectRequirement {
	protected final Aspect aspect;
	protected final int requirement;
	
	public AspectRequirement(Aspect aspect, int requirement) {
		this.aspect = aspect;
		this.requirement = requirement;
	}
	
	public abstract boolean checkRequirement(int level); 
}
