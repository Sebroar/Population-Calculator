package sebroar.populationcalculator.data;

public record Religion(String name, double religionPower,
		double growthModifierAdditive, double growthModifierMultiplicative) {
	public static final Religion NULL_RELIGION = new Religion("NULL", 0, 0, 0);
}
