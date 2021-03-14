package com.dreamless.brewery.brew;

public class AspectRarityPair {
	public final Aspect aspect;
	public final Rarity rarity;
	
	public  AspectRarityPair(Aspect aspect, Rarity rarity) {
		this.aspect = aspect;
		this.rarity = rarity;
	}
	
	@Override
	public String toString() {
		return "AspectRarityPair [aspect=" + aspect + ", rarity=" + rarity + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aspect == null) ? 0 : aspect.hashCode());
		result = prime * result + ((rarity == null) ? 0 : rarity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AspectRarityPair other = (AspectRarityPair) obj;
		if (aspect != other.aspect)
			return false;
		if (rarity != other.rarity)
			return false;
		return true;
	}
	
}
