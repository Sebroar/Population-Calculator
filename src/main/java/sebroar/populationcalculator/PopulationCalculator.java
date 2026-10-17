package sebroar.populationcalculator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.filechooser.FileSystemView;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import sebroar.populationcalculator.data.*;
import sebroar.populationcalculator.utils.CommandLineParser;

public class PopulationCalculator {
	public static File dir;
	public static File log;
	public static PrintStream logger;
	public static ArrayList<Culture> cultures = new ArrayList<Culture>(0);
	public static ArrayList<Culture> primaryCultures = new ArrayList<Culture>(0);
	public static ArrayList<Language> languages = new ArrayList<Language>(0);
	public static ArrayList<Language> officialLanguages = new ArrayList<Language>(0);
	public static ArrayList<Religion> religions = new ArrayList<Religion>(0);
	public static ArrayList<Religion> stateReligions = new ArrayList<Religion>(0);
	public static ArrayList<Species> species = new ArrayList<Species>(0);
	public static ArrayList<Location> locations = new ArrayList<Location>(0);
	public static double growthRate;
	public static long timesteps;
	public static double conversionRate;
	public static double stateReligionPowerMultiplier;
	public static double assimilationRate;
	public static double primaryCulturePowerMultiplier;
	public static double languageShiftRate;
	public static double officialLanguagePowerMultiplier;
	static ObjectMapper mapper = JsonMapper.builder().configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
			.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
			.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true)
			.configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
			.build();
	
	public static void main(String[] args) {
		// Initial 
		String defaultDirectory = FileSystemView.getFileSystemView().getDefaultDirectory().toString();
		String separator = File.separator;
		CommandLineParser clp = new CommandLineParser(args);
		
		// Get working directory
		if (clp.getArgumentNames().contains("d")) {
			dir = new File(clp.getArgumentValue("d")[0]);
		} else {
			Scanner sc = new Scanner(System.in);
			System.out.print("""
					No path to a directory for file storage has been specified.
					Do you wish to specify one now? (y/n)
					""");
			String cont = null;
			while (cont == null) {
				String unvalidatedString = sc.next();
				if (unvalidatedString.equalsIgnoreCase("y")) {
					cont = "y";
					break;
				} else if (unvalidatedString.equalsIgnoreCase("n")) {
					cont = "n";
					break;
				} else {
					System.out.println("Please enter 'y' or 'n'");
				}
			}
			if (cont == "y") {
				cont = null;
				System.out.println("Please enter the path to the directory");
				while (cont == null) {
					String unvalidatedString = sc.next();
					try {
						Paths.get(unvalidatedString);
						dir = new File(unvalidatedString);
						break;
					} catch (InvalidPathException e) {
						System.out.println("Please enter a valid path");
					}
				}
			} else {
				dir = new File(defaultDirectory, "Pop calculator");
				System.out.println(
						"The program will create a new folder in the default directory ("
								+ defaultDirectory + ")");
			}
			sc.close();
		}
		
		// Create the working directory if it doesn't exist yet, and create log file
		dir.mkdirs();
		log = new File(dir, "logs" + separator + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")) + ".log");
		log.mkdirs();
		
		// No log file = bad = stop the program
		try {
			log.createNewFile();
			logger = new PrintStream(log);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not generate log file. Exiting program");
			e.printStackTrace();
			return;
		}
		if (clp.getFlag("genExample")) {
			printLog(LogEntryType.INFO, "The program will now generate example files");
			// TODO Generate example files
			printLog(LogEntryType.INFO, "Example files generated, the program will now terminate");
			return;
		}
		
		// Load user-defined data
		loadLocations();
		loadLanguages();
		loadReligions();
		loadSpecies();
		loadCultures();
		loadPopulation();
		
		// Load config file
		try {
			loadConfig();
		} catch (IOException e) {
			printLog(LogEntryType.FATAL, """
					Failed to load config.json
					---------- Fatal error occurred! ----------""");
			e.printStackTrace();
			e.printStackTrace(logger);
			return;
		}
		
		// Finally do the simulation
		long t = 0;
		while (t < timesteps) {
			for (Location l : locations) {
				l.growPopulation(growthRate);
				l.assimilatePopulation(assimilationRate, primaryCultures, primaryCulturePowerMultiplier);
				l.convertPopulation(conversionRate, stateReligions, stateReligionPowerMultiplier);
				l.spreadMainLanguage(languageShiftRate, officialLanguages, officialLanguagePowerMultiplier);
				printLog(LogEntryType.INFO, "Timestep " + t + " was completed");
			}
			t++;
		}
		
		// Output result
		printLog(LogEntryType.INFO, "Creating output file");
		File output = new File(dir, "output.csv");
		PrintStream out;
		try {
			output.createNewFile();
			printLog(LogEntryType.INFO, "output.csv was created successfully");
			out = new PrintStream(output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printLog(LogEntryType.FATAL, """
					Couldn't create output.csv
					---------- Fatal error occurred! ----------""");
			e.printStackTrace();
			e.printStackTrace(logger);
			return;
		}
		printLog(LogEntryType.INFO, "Printing output to output.csv");
		out.println("Location,Culture,Religion,Language,Species,Size");
		for (Location l : locations) {
			for (PopulationGroup pop : l.population) {
				out.println(
						l.getName() + "," + pop.getCulture().name() + ","
						+ pop.getReligion().name() + "," + pop.getLanguage().name() + ","
						+ pop.getSpecies().name() + "," + pop.getSize()
				);
			}
		}
		out.close();
		printLog(LogEntryType.INFO, "Output printed to output.csv successfully");
		printLog(LogEntryType.INFO, "Program executed successfully");
		logger.close();
	}
	
	private static void loadCultures() {
		cultures.add(Culture.NULL_CULTURE);
		try {
			JsonNode arrayNode = mapper.readTree(new File(dir, "cultures.json")).get("cultures");
			if (arrayNode.isArray()) {
				for (JsonNode cul : arrayNode) {
					Language defLang = Language.NULL_LANGUAGE;
					Religion defRel = Religion.NULL_RELIGION;
					Species defSp = Species.NULL_SPECIES;
					for (Language lang : languages) {
						if (lang.name().equals(cul.get("default_language").asText())) {
							defLang = lang;
							break;
						}
					}
					for (Religion rel : religions) {
						if (rel.name().equals(cul.get("default_religion").asText())) {
							defRel = rel;
							break;
						}
					}
					for (Species sp : species) {
						if (sp.name().equals(cul.get("default_species").asText())) {
							defSp = sp;
							break;
						}
					}
					double add = cul.has("growth_rate_modifier_additive") ? 
							cul.get("growth_rate_modifier_additive").asDouble() : 0;
					double mult = cul.has("growth_rate_modifier_multiplicative") ?
							cul.get("growth_rate_modifier_multiplicative").asDouble() : 0;
					cultures.add(new Culture(cul.get("name").asText(),
							cul.get("culture_power").asDouble(0), defLang, defRel, defSp, add, mult));
				}
				cultures.sort((a, b) -> {return a.name().compareTo(b.name());});
				printLog(LogEntryType.INFO, "cultures.json was loaded successfully");
			} else {
				JsonNodeType t = arrayNode.getNodeType();
				printLog(LogEntryType.WARNING, "Couldn't load cultures.json: " 
						+ t.toString().toLowerCase() + " was found, while array was expected");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printLog(LogEntryType.WARNING, "Couldn't load cultures.json");
			e.printStackTrace(logger);
			e.printStackTrace();
			return;
		}
	}
	
	private static void loadLanguages() {
		languages.add(Language.NULL_LANGUAGE);
		try {
			JsonNode arrayNode = mapper.readTree(new File(dir, "languages.json")).get("languages");
			if (arrayNode.isArray()) {
				for (JsonNode lang : arrayNode) {
					languages.add(new Language(lang.get("name").asText(), lang.get("language_power").asDouble()));
				}
				languages.sort((a, b) -> {return a.name().compareTo(b.name());});
				printLog(LogEntryType.INFO, "languages.json was loaded successfully");
			} else {
				JsonNodeType t = arrayNode.getNodeType();
				printLog(LogEntryType.WARNING, "Couldn't load languages.json: " 
						+ t.toString().toLowerCase() + " was found, while array was expected");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printLog(LogEntryType.WARNING, "Couldn't load languages.json");
			e.printStackTrace(logger);
			e.printStackTrace();
			return;
		}
	}
	
	private static void loadReligions() {
		religions.add(Religion.NULL_RELIGION);
		try {
			JsonNode arrayNode = mapper.readTree(new File(dir, "religions.json")).get("religions");
			if (arrayNode.isArray()) {
				for (JsonNode rel : arrayNode) {
					double add = rel.has("growth_rate_modifier_additive") ? 
							rel.get("growth_rate_modifier_additive").asDouble() : 0;
					double mult = rel.has("growth_rate_modifier_multiplicative") ?
							rel.get("growth_rate_modifier_multiplicative").asDouble() : 0;
					religions.add(new Religion(rel.get("name").asText(), 
							rel.get("religion_power").asDouble(), add, mult));
				}
				religions.sort((a, b) -> {return a.name().compareTo(b.name());});
				printLog(LogEntryType.INFO, "religions.json was loaded successfully");
			} else {
				JsonNodeType t = arrayNode.getNodeType();
				printLog(LogEntryType.WARNING, "Couldn't load religions.json: " 
						+ t.toString().toLowerCase() + " was found, while array was expected");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printLog(LogEntryType.WARNING, "Couldn't load religions.json");
			e.printStackTrace(logger);
			e.printStackTrace();
			return;
		}
	}
	
	private static void loadLocations() {
		locations.add(Location.NULL_LOCATION);
		try {
			JsonNode arrayNode = mapper.readTree(new File(dir, "locations.json")).get("locations");
			if (arrayNode.isArray()) {
				for (JsonNode loc : arrayNode) {
					double add = loc.has("growth_rate_modifier_additive") ? 
							loc.get("growth_rate_modifier_additive").asDouble() : 0;
					double mult = loc.has("growth_rate_modifier_multiplicative") ?
							loc.get("growth_rate_modifier_multiplicative").asDouble() : 0;
					locations.add(new Location(loc.get("name").asText(), add, mult));
				}
				locations.sort((a, b) -> {return a.getName().compareTo(b.getName());});
				printLog(LogEntryType.INFO, "locations.json was loaded successfully");
			} else {
				JsonNodeType t = arrayNode.getNodeType();
				printLog(LogEntryType.WARNING, "Couldn't load locations.json: " 
						+ t.toString().toLowerCase() + " was found, while array was expected");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printLog(LogEntryType.WARNING, "Couldn't load locations.json");
			e.printStackTrace(logger);
			e.printStackTrace();
			return;
		}
	}
	
	private static void loadSpecies() {
		species.add(Species.NULL_SPECIES);
		try {
			JsonNode arrayNode = mapper.readTree(new File(dir, "species.json")).get("species");
			if (arrayNode.isArray()) {
				for (JsonNode sp : arrayNode) {
					double add = sp.has("growth_rate_modifier_additive") ? 
							sp.get("growth_rate_modifier_additive").asDouble() : 0;
					double mult = sp.has("growth_rate_modifier_multiplicative") ?
							sp.get("growth_rate_modifier_multiplicative").asDouble() : 0;
					locations.add(new Location(sp.get("name").asText(), add, mult));
				}
				species.sort((a, b) -> {return a.name().compareTo(b.name());});
				printLog(LogEntryType.INFO, "species.json was loaded successfully");
			} else {
				JsonNodeType t = arrayNode.getNodeType();
				printLog(LogEntryType.WARNING, "Couldn't load species.json: " 
						+ t.toString().toLowerCase() + " was found, while array was expected");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printLog(LogEntryType.WARNING, "Couldn't load species.json");
			e.printStackTrace(logger);
			e.printStackTrace();
			return;
		}
	}
	
	private static void loadPopulation() {
		try {
			JsonNode arrayNode = 
					mapper.readTree(new File(dir, "population_groups.json")).get("population_groups");
			if (arrayNode.isArray()) {
				for (JsonNode pop : arrayNode) {
					Location loc = Location.NULL_LOCATION;
					Culture cul = Culture.NULL_CULTURE;
					Religion rel = Religion.NULL_RELIGION;
					Language lang = Language.NULL_LANGUAGE;
					Species sp = Species.NULL_SPECIES;
					long size = pop.get("size").asLong();
					for (Location location: locations) {
						if (pop.get("location").asText().equals(location.getName())) {
							loc = location;
							break;
						}
					}
					for (Culture culture: cultures) {
						if (pop.get("culture").asText().equals(culture.name())) {
							cul = culture;
							break;
						}
					}
					for (Religion religion: religions) {
						if (pop.get("religion").asText().equals(religion.name())) {
							rel = religion;
							break;
						}
					}
					for (Language language: languages) {
						if (pop.get("language").asText().equals(language.name())) {
							lang = language;
							break;
						}
					}
					for (Species spValue: species) {
						if (pop.get("species").asText().equals(spValue.name())) {
							sp = spValue;
							break;
						}
					}
					loc.population.add(new PopulationGroup(loc, cul, lang, rel, sp, size));
				}
				printLog(LogEntryType.INFO, "population_groups.json was loaded successfully");
			} else {
				JsonNodeType t = arrayNode.getNodeType();
				printLog(LogEntryType.WARNING, "Couldn't load population_groups.json: " 
						+ t.toString().toLowerCase() + " was found, while array was expected");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printLog(LogEntryType.WARNING, "Couldn't load population_groups.json");
			e.printStackTrace(logger);
			e.printStackTrace();
			return;
		}
	}
	
	public static void loadConfig() throws IOException {
		var cfg = new File(dir, "config.json");
		growthRate = mapper.readTree(cfg).get("global_growth_rate").asDouble() / 100;
		timesteps = mapper.readTree(cfg).get("timesteps").asLong();
		JsonNode pcNode = mapper.readTree(cfg).get("primary_cultures");
		if (pcNode.isArray()) {
			for (JsonNode pc : pcNode) {
				for (Culture c : cultures) {
					if (c.name() == pc.asText()) {
						primaryCultures.add(c);
					}
				}
			}
			primaryCultures.sort((a, b) -> {return a.name().compareTo(b.name());});
		}
		primaryCulturePowerMultiplier = mapper.readTree(cfg).get("primary_culture_power_multiplier").asDouble();
		conversionRate = mapper.readTree(cfg).get("conversion_rate").asDouble() / 100;
		JsonNode srNode = mapper.readTree(cfg).get("state_religions");
		if (srNode.isArray()) {
			for (JsonNode sr : srNode) {
				for (Religion r : religions) {
					if (r.name() == sr.asText()) {
						stateReligions.add(r);
					}
				}
			}
			stateReligions.sort((a, b) -> {return a.name().compareTo(b.name());});
		}
		stateReligionPowerMultiplier = mapper.readTree(cfg).get("state_religion_power_multiplier").asDouble();
		JsonNode olNode = mapper.readTree(cfg).get("primary_cultures");
		if (olNode.isArray()) {
			for (JsonNode ol : olNode) {
				for (Language l : languages) {
					if (l.name() == ol.asText()) {
						officialLanguages.add(l);
					}
				}
			}
			officialLanguages.sort((a, b) -> {return a.name().compareTo(b.name());});
		}
		primaryCulturePowerMultiplier = mapper.readTree(cfg).get("primary_culture_power_multiplier").asDouble();
		assimilationRate = mapper.readTree(cfg).get("assimilation_rate").asDouble() / 100;
		languageShiftRate = mapper.readTree(cfg).get("language_shift_rate").asDouble() / 100;
		printLog(LogEntryType.INFO, "config.json was loaded successfully");
	}
	
	public static void printLog(LogEntryType t, String e) {
		System.out.println(e);
		logger.println("[ " + t + " " 
				+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + e);
	}
	
	enum LogEntryType {
		INFO,
		WARNING,
		FATAL
	}
}
