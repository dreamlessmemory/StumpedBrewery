package com.dreamless.brewery.brew;

import java.util.HashMap;

public class BreweryEffectAspectDistribution {
	
	public HashMap<Aspect, Double> multiplierMap = new HashMap<Aspect, Double>();
	public BreweryEffectAspectDistribution() {}
	
	public void addMultipler(Aspect aspect, double multipler) {
		multiplierMap.put(aspect, multipler);
	}

}
