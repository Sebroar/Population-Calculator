package sebroar.populationcalculator.data;

import java.util.ArrayList;
import java.util.Arrays;
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
	
	public void assimilatePopulation(double rate) {
		var totalPopulation = getTotalPopulation();
		var pByCul = new TreeMap<Culture, ArrayList<PopulationGroup>>((a, b) -> a.name().compareTo(b.name()));
		for (PopulationGroup p : population) {
			Culture c = p.getCulture();
			if (pByCul.containsKey(c)) {
				pByCul.get(c).add(p);
			} else {
				pByCul.put(c, new ArrayList<PopulationGroup>());
				pByCul.get(c).add(p);
			}
		}
//		var culPower = new double[pByCul.size()];
		var culs = new Culture[pByCul.size()];
		var culPop = new long[pByCul.size()];
		pByCul.keySet().toArray(culs);
		for (Culture c : culs) {
//			culPower[Arrays.asList(culs).indexOf(c)] = c.culturePower();
			for (PopulationGroup p : pByCul.get(c)) {
				culPop[Arrays.asList(culs).indexOf(c)] = p.getSize();
			}
		}
		var culByPow = Arrays.asList(culs);
		culByPow.sort((a, b) -> Double.compare(a.culturePower(), b.culturePower()));
		var culEffPow = new double[pByCul.size()];
		var culByEffPow = Arrays.asList(culs);
		for (int i = 0; i < culEffPow.length; i++) {
			culEffPow[i] = culPop[i] * culs[i].culturePower();
		}
		culByEffPow.sort((a, b) -> Double.compare(
				culs[Arrays.asList(culs).indexOf(a)].culturePower() * culPop[Arrays.asList(culs).indexOf(a)], 
				culs[Arrays.asList(culs).indexOf(b)].culturePower() * culPop[Arrays.asList(culs).indexOf(b)]
		));
		
	}
}
