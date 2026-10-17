package sebroar.populationcalculator.data;

public record Language(String name, double languagePower) {
	public static final Language NULL_LANGUAGE = new Language("NULL", 0);
}
