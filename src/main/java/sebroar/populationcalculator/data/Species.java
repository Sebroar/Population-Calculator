package sebroar.populationcalculator.data;

public record Species(String name, double growthModifierAdditive, double growthModifierMultiplicative) {
	public static final Species NULL_SPECIES = new Species("NULL", 0, 0);
}
