package sebroar.populationcalculator.data;

import java.util.ArrayList;
import java.util.Arrays;
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
		var culs = new Culture[pByCul.size()];
		var culPop = new long[pByCul.size()];
		pByCul.keySet().toArray(culs);
		for (Culture c : culs) {
			for (var p : pByCul.get(c)) {
				culPop[Arrays.asList(culs).indexOf(c)] = p.getSize();
			}
		}
		var culEffPow = new double[pByCul.size()];
		var culByEffPow = Arrays.asList(culs);
		for (int i = 0; i < culEffPow.length; i++) {
			double mult = primaryCultures.contains(culs[i]) ? pcMult : 1;
			culEffPow[i] = culPop[i] * culs[i].culturePower() * mult / totalPopulation;
		}
		culByEffPow.sort((a, b) -> Double.compare(culEffPow[Arrays.asList(culs).indexOf(a)], 
						culEffPow[Arrays.asList(culs).indexOf(b)]));
		var mainCul = culByEffPow.getFirst();
		var mainCulEffPow = culEffPow[Arrays.asList(culs).indexOf(mainCul)];
		for (var p : population) {
			var cEffPow = culEffPow[Arrays.asList(culs).indexOf(p.getCulture())];
			if (p.getCulture() != mainCul && cEffPow < mainCulEffPow) {
				long change = Math.min(p.getSize(), (long) Math.ceil(p.getSize() * rate / cEffPow));
				p.changeSize(-change);
				addPopulationGroup(mainCul, p.getLanguage(), p.getReligion(), p.getSpecies(), change);
				if (p.getSize() < 1) {
					population.remove(p);
				}
			}
		}
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
		var Rels = new Religion[pByRel.size()];
		var RelPop = new long[pByRel.size()];
		pByRel.keySet().toArray(Rels);
		for (var r : Rels) {
			for (var p : pByRel.get(r)) {
				RelPop[Arrays.asList(Rels).indexOf(r)] = p.getSize();
			}
		}
		var relEffPow = new double[pByRel.size()];
		var RelByEffPow = Arrays.asList(Rels);
		for (int i = 0; i < relEffPow.length; i++) {
			double mult = stateReligion.contains(Rels[i]) ? srMult : 1;
			relEffPow[i] = RelPop[i] * Rels[i].religionPower() * mult / totalPopulation;
		}
		RelByEffPow.sort((a, b) -> Double.compare(relEffPow[Arrays.asList(Rels).indexOf(a)], 
						relEffPow[Arrays.asList(Rels).indexOf(b)]));
		var mainRel = RelByEffPow.getFirst();
		var mainRelEffPow = relEffPow[Arrays.asList(Rels).indexOf(mainRel)];
		for (var p : population) {
			var rEffPow = relEffPow[Arrays.asList(Rels).indexOf(p.getReligion())];
			if (p.getReligion() != mainRel && rEffPow < mainRelEffPow) {
				long change = Math.min(p.getSize(), (long) Math.ceil(p.getSize() * rate / rEffPow));
				p.changeSize(-change);
				addPopulationGroup(p.getCulture(), p.getLanguage(), p.getReligion(), p.getSpecies(), change);
				if (p.getSize() < 1) {
					population.remove(p);
				}
			}
		}
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
		var langs = new Language[pByLang.size()];
		var langPop = new long[pByLang.size()];
		pByLang.keySet().toArray(langs);
		for (var c : langs) {
			for (var p : pByLang.get(c)) {
				langPop[Arrays.asList(langs).indexOf(c)] = p.getSize();
			}
		}
		var langEffPow = new double[pByLang.size()];
		var langByEffPow = Arrays.asList(langs);
		for (int i = 0; i < langEffPow.length; i++) {
			double mult = officialLanguage.contains(langs[i]) ? olMult : 1;
			langEffPow[i] = langPop[i] * langs[i].languagePower() * mult / totalPopulation;
		}
		langByEffPow.sort((a, b) -> Double.compare(langEffPow[Arrays.asList(langs).indexOf(a)], 
						langEffPow[Arrays.asList(langs).indexOf(b)]));
		var mainLang = langByEffPow.getFirst();
		var mainLangEffPow = langEffPow[Arrays.asList(langs).indexOf(mainLang)];
		for (var p : population) {
			var cEffPow = langEffPow[Arrays.asList(langs).indexOf(p.getLanguage())];
			if (p.getLanguage() != mainLang && cEffPow < mainLangEffPow) {
				long change = Math.min(p.getSize(), (long) Math.ceil(p.getSize() * rate / cEffPow));
				p.changeSize(-change);
				addPopulationGroup(p.getCulture(), mainLang, p.getReligion(), p.getSpecies(), change);
				if (p.getSize() < 1) {
					population.remove(p);
				}
			}
		}
	}
}
