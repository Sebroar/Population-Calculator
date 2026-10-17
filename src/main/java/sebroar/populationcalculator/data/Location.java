package sebroar.populationcalculator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class Location {
	private final String name;
	private final double growthModifierAdditive;
	private final double growthModifierMultiplicative;
	public ArrayList<PopulationGroup> population = new ArrayList<PopulationGroup>(0);
	public static final Location NULL_LOCATION = new Location("NULL", 0 , 0);
	
	public Location(String name, double rateModifierAdditive, double rateModifierMultiplicative) {
		this.name = name;
		this.growthModifierAdditive = rateModifierAdditive;
		this.growthModifierMultiplicative = rateModifierMultiplicative;
	}
	
	public String getName() {
		return name;
	}
	
	public long getTotalPopulation() {
		long tot = 0;
		for (PopulationGroup p : population) {
			tot += p.getSize();
		}
		return tot;
	}
	
	public void growPopulation(double rate) {
		rate = rate * (1 + growthModifierMultiplicative) + growthModifierAdditive;
		for (PopulationGroup p : population) {
			p.growSize(rate);
			if (p.getSize() < 1) {
				population.remove(p);
			}
		}
	}
	
	public PopulationGroup findPopulationGroup(Culture culture, Language language, 
			Religion religion, Species species) {
		for (PopulationGroup p : population) {
			if (p.getCulture() == culture && p.getLanguage() == language
					&& p.getReligion() == religion && p.getSpecies() == species) {
				return p;
			}
		}
		return null;
	}
	
	public void addPopulationGroup(Culture culture, Language language, 
			Religion religion, Species species, long size) {
		PopulationGroup pop = findPopulationGroup(culture, language, religion, species);
		if (pop == null) {
			population.add(new PopulationGroup(this, culture, language, religion, species, size));
		} else {
			pop.changeSize(size);
		}
	}
	
	public void assimilatePopulation(double rate, List<Culture> primaryCultures, double pcMult) {
		var totalPopulation = getTotalPopulation();
		var pByCul = new TreeMap<Culture, ArrayList<PopulationGroup>>((a, b) -> a.name().compareTo(b.name()));
		for (var p : population) {
			Culture c = p.getCulture();
			if (pByCul.containsKey(c)) {
				pByCul.get(c).add(p);
			} else {
				pByCul.put(c, new ArrayList<PopulationGroup>());
				pByCul.get(c).add(p);
			}
		}
		ArrayList<Culture> culs = new ArrayList<Culture>(0);
		var culPop = new long[pByCul.size()];
		culs.addAll(pByCul.keySet());
		for (Culture c : culs) {
			for (var p : pByCul.get(c)) {
				culPop[culs.indexOf(c)] = p.getSize();
			}
		}
		var culEffPow = new double[pByCul.size()];
		var culByEffPow = new ArrayList<Culture>(0);
		culByEffPow.addAll(culs);
		for (int i = 0; i < culEffPow.length; i++) {
			double mult = primaryCultures.contains(culs.get(i)) ? pcMult : 1;
			culEffPow[i] = culPop[i] * culs.get(i).culturePower() * mult / totalPopulation;
		}
		culByEffPow.sort((a, b) -> -1 * Double.compare(culEffPow[culs.indexOf(a)], culEffPow[culs.indexOf(b)]));
		var mainCul = culByEffPow.get(0);
		var mainCulEffPow = culEffPow[culs.indexOf(mainCul)];
		for (var p : population) {
			var cEffPow = culEffPow[culs.indexOf(p.getCulture())];
			if (p.getCulture() != mainCul && cEffPow < mainCulEffPow) {
				long change = Math.min(p.getSize(), (long) Math.ceil(p.getSize() * rate / cEffPow));
				p.changeSize(-change);
				addPopulationGroup(mainCul, p.getLanguage(), p.getReligion(), p.getSpecies(), change);
			}
		}
		population.removeIf(p -> p.getSize() < 1);
	}
	
	public void convertPopulation(double rate, List<Religion> stateReligion, double srMult) {
		var totalPopulation = getTotalPopulation();
		var pByRel = new TreeMap<Religion, ArrayList<PopulationGroup>>((a, b) -> a.name().compareTo(b.name()));
		for (var p : population) {
			Religion r = p.getReligion();
			if (pByRel.containsKey(r)) {
				pByRel.get(r).add(p);
			} else {
				pByRel.put(r, new ArrayList<PopulationGroup>());
				pByRel.get(r).add(p);
			}
		}
		ArrayList<Religion> rels = new ArrayList<Religion>(0);
		var RelPop = new long[pByRel.size()];
		rels.addAll(pByRel.keySet());
		for (var r : rels) {
			for (var p : pByRel.get(r)) {
				RelPop[rels.indexOf(r)] = p.getSize();
			}
		}
		var relEffPow = new double[pByRel.size()];
		var relByEffPow = new ArrayList<Religion>(0);
		relByEffPow.addAll(rels);
		for (int i = 0; i < relEffPow.length; i++) {
			double mult = stateReligion.contains(rels.get(i)) ? srMult : 1;
			relEffPow[i] = RelPop[i] * rels.get(i).religionPower() * mult / totalPopulation;
		}
		relByEffPow.sort((a, b) -> -1 * Double.compare(relEffPow[rels.indexOf(a)], 
						relEffPow[rels.indexOf(b)]));
		var mainRel = relByEffPow.get(0);
		var mainRelEffPow = relEffPow[rels.indexOf(mainRel)];
		for (var p : population) {
			var rEffPow = relEffPow[rels.indexOf(p.getReligion())];
			if (p.getReligion() != mainRel && rEffPow < mainRelEffPow) {
				long change = Math.min(p.getSize(), (long) Math.ceil(p.getSize() * rate / rEffPow));
				p.changeSize(-change);
				addPopulationGroup(p.getCulture(), p.getLanguage(), p.getReligion(), p.getSpecies(), change);
			}
		}
		population.removeIf(p -> p.getSize() < 1);
	}
	
	public void spreadMainLanguage(double rate, List<Language> officialLanguage, double olMult) {
		var totalPopulation = getTotalPopulation();
		var pByLang = new TreeMap<Language, ArrayList<PopulationGroup>>((a, b) -> a.name().compareTo(b.name()));
		for (var p : population) {
			var c = p.getLanguage();
			if (pByLang.containsKey(c)) {
				pByLang.get(c).add(p);
			} else {
				pByLang.put(c, new ArrayList<PopulationGroup>());
				pByLang.get(c).add(p);
			}
		}
		ArrayList<Language> langs = new ArrayList<Language>(0);
		var langPop = new long[pByLang.size()];
		langs.addAll(pByLang.keySet());
		for (var c : langs) {
			for (var p : pByLang.get(c)) {
				langPop[langs.indexOf(c)] = p.getSize();
			}
		}
		var langEffPow = new double[pByLang.size()];
		var langByEffPow = new ArrayList<Language>(0);
		langByEffPow.addAll(langs);
		for (int i = 0; i < langEffPow.length; i++) {
			double mult = officialLanguage.contains(langs.get(i)) ? olMult : 1;
			langEffPow[i] = langPop[i] * langs.get(i).languagePower() * mult / totalPopulation;
		}
		langByEffPow.sort((a, b) -> -1 * Double.compare(langEffPow[langs.indexOf(a)], 
						langEffPow[langs.indexOf(b)]));
		var mainLang = langByEffPow.get(0);
		var mainLangEffPow = langEffPow[langs.indexOf(mainLang)];
		for (var p : population) {
			var cEffPow = langEffPow[langs.indexOf(p.getLanguage())];
			if (p.getLanguage() != mainLang && cEffPow < mainLangEffPow) {
				long change = Math.min(p.getSize(), (long) Math.ceil(p.getSize() * rate / cEffPow));
				p.changeSize(-change);
				addPopulationGroup(p.getCulture(), mainLang, p.getReligion(), p.getSpecies(), change);
			}
		}
		population.removeIf(p -> p.getSize() < 1);
	}
}
