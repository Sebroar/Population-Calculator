package sebroar.populationcalculator.data;

public class PopulationGroup {
	private final Location location;
	private final Culture culture;
	private final Language language;
	private final Religion religion;
	private final Species species;
	private long size;
	
	public PopulationGroup(Location location, Culture culture, Language language, 
			Religion religion, Species species, long size) {
		this.location = location;
		this.culture = culture;
		this.language = language;
		this.religion = religion;
		this.species = species;
		this.setSize(size);
	}
	
	public Location getLocation() {
		return location;
	}
	
	public Culture getCulture() {
		return culture;
	}
	
	public Language getLanguage() {
		return language;
	}
	
	public Religion getReligion() {
		return religion;
	}
	
	public Species getSpecies() {
		return species;
	}
	
	public long getSize() {
		return size;
	}
	
	public void setSize(long size) {
		this.size = size;
	}
	
	public void growSize(double rate) {
		double multSum = culture.growthModifierMultiplicative() 
				+ religion.growthModifierMultiplicative() + species.growthModifierMultiplicative();
		double addSum = culture.growthModifierAdditive() + religion.growthModifierAdditive()
				+ species.growthModifierAdditive();
		rate = rate * (1 + multSum) + addSum;
		this.size *= (1 + rate);
	}
	
	public void changeSize(long change) {
		this.size += change;
	}
}
