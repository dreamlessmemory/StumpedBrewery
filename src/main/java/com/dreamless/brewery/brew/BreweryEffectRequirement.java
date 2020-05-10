package com.dreamless.brewery.brew;

import java.util.HashMap;

public class BreweryEffectRequirement{
	
	public static final int MAXIMUM_STACKS = 3;
	public static final int MAXIMUM_TOTAL_STACKS = MAXIMUM_STACKS * 6;
	
	public final int totalMax;
	public HashMap<Aspect, AspectRequirement> aspectRequriementMap;
	public BreweryEffectRequirement(int minimumDistillCycles) {
		totalMax = MAXIMUM_TOTAL_STACKS - minimumDistillCycles;
	}
	public void addAspectMaximalRequirement(Aspect aspect, int max) {
		aspectRequriementMap.put(aspect, new AspectMaximalRequirement(aspect, max));
	}
	public void addAspectMinimalRequirement(Aspect aspect, int min) {
		aspectRequriementMap.put(aspect, new AspectMinimalRequirement(aspect, min));
	}
	
	public boolean checkAspectRequirement(Aspect aspect, int level) {
		AspectRequirement aspectRequirement = aspectRequriementMap.get(aspect);
		if(aspectRequirement == null) {
			return true;
		} else {
			return aspectRequirement.checkRequirement(level);
		}
	}
}