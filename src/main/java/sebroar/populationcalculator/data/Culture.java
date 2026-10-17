package sebroar.populationcalculator.data;

public record Culture(String name, double culturePower, Language defaultLanguage, Religion defaultReligion,
		Species defaultSpecies, double growthModifierAdditive, double growthModifierMultiplicative) {
	public static final Culture NULL_CULTURE = 
			new Culture("NULL", Double.MIN_NORMAL, Language.NULL_LANGUAGE, 
					Religion.NULL_RELIGION, Species.NULL_SPECIES, 0, 0);
}
