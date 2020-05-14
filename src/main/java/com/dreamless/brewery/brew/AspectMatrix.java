package com.dreamless.brewery.brew;

import java.util.HashMap;

public class AspectMatrix {
	private HashMap<Aspect, Integer> aspectMatrix;
	private int totalCount = BreweryEffectRequirement.MAXIMUM_TOTAL_STACKS;
	public AspectMatrix() {
		aspectMatrix.put(Aspect.LITHIC, 3);
		aspectMatrix.put(Aspect.INFERNAL, 3);
		aspectMatrix.put(Aspect.PYROTIC, 3);
		aspectMatrix.put(Aspect.AERIAL, 3);
		aspectMatrix.put(Aspect.VOID, 3);
		aspectMatrix.put(Aspect.AQUATIC, 3);
	}
	public int getTotalCount() {
		return totalCount;
	}
	public void distillAspect(Aspect aspect, int amount) {
		aspectMatrix.put(aspect, Math.max(aspectMatrix.get(aspect) - amount, 0));
	}
	public int getAspectLevel(Aspect aspect) {
		return aspectMatrix.get(aspect);
	}
}
