package com.kurento.ktool.rom.processor.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.kurento.ktool.rom.processor.json.JsonModelSaverLoader;
import com.kurento.ktool.rom.processor.model.Model;

import freemarker.template.TemplateException;

public class Main {

	private static final String HELP = "h";
	private static final String VERBOSE = "v";
	private static final String ROM = "r";
	private static final String DEPROM = "dr";
	private static final String TEMPLATES = "t";
	private static final String CODEGEN = "c";
	private static final String DELETE = "d";
	private static final String CONFIG = "cf";

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException,
			TemplateException {

		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		options.addOption(VERBOSE, "verbose", false,
				"Prints source code while being generated.");

		options.addOption(HELP, "help", false, "Prints this message.");

		options.addOption(OptionBuilder.withLongOpt("rom")
				.withDescription("Remote object model description file.")
				.hasArg().withArgName("ROM_FILE").isRequired().create(ROM));

		options.addOption(OptionBuilder
				.withLongOpt("deprom")
				.withDescription(
						"Remote object model description files used as dependencies.")
				.hasArg().withArgName("DEP_ROM_FILE").isRequired()
				.create(DEPROM));

		options.addOption(OptionBuilder.withLongOpt("templates")
				.withDescription("Directory that contains template files.")
				.hasArg().withArgName("TEMPLATES_DIR").isRequired()
				.create(TEMPLATES));

		options.addOption(OptionBuilder.withLongOpt("codegen")
				.withDescription("Destination directory for generated files.")
				.hasArg().withArgName("CODEGEN_DIR").isRequired()
				.create(CODEGEN));

		options.addOption(DELETE, "delete", false,
				"Delete destination directory before generating files.");

		options.addOption(OptionBuilder.withLongOpt("config")
				.withDescription("Configuration file.").hasArg()
				.withArgName("CONFIGURATION_FILE").create(CONFIG));

		CommandLine line = null;

		try {
			line = parser.parse(options, args);

			if (line.hasOption(HELP) || !line.hasOption(ROM)
					|| !line.hasOption(TEMPLATES) || !line.hasOption(CODEGEN)) {
				printHelp(options);
				System.exit(0);
			}
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			printHelp(options);
			System.exit(1);
		}

		File romFile = getRomFile(line);
		List<File> dependencyRomFiles = getDependencyRomFile(line);
		JsonObject configContent = getConfigContent(line);
		File templatesDir = getTemplatesDir(line);
		File codegenDir = getCodegenDir(line);
		boolean verbose = line.hasOption(VERBOSE);
		boolean delete = line.hasOption(DELETE);

		generateCode(delete, verbose, romFile, dependencyRomFiles,
				configContent, templatesDir, codegenDir);

	}

	public static void generateCode(boolean delete, boolean verbose,
			File romFile, List<File> dependencyRomFiles,
			JsonObject configContent, File templatesDir, File codegenDir)
			throws IOException, FileNotFoundException, TemplateException {

		deleteIfNecessary(delete, configContent, codegenDir);

		List<Model> dependencyModels = new ArrayList<Model>();
		for (File dependencyRomFile : dependencyRomFiles) {
			Model depModel = JsonModelSaverLoader.getInstance().loadFromFile(
					dependencyRomFile);
			depModel.populateModel();
			dependencyModels.add(depModel);
		}

		Model model = JsonModelSaverLoader.getInstance().loadFromFile(romFile);
		model.populateModel(dependencyModels);

		CodeGen codeGen = new CodeGen(templatesDir, codegenDir, verbose,
				configContent);

		if (configContent.has("expandMethodsWithOpsParams")
				&& configContent.get("expandMethodsWithOpsParams")
						.getAsBoolean()) {
			model.expandMethodsWithOpsParams();
		}

		codeGen.generateCode(model);

		System.out.println("Generation complete");
	}

	private static List<File> getDependencyRomFile(CommandLine line) {
		String[] files = line.getOptionValue(DEPROM).split(",");
		List<File> depRomFiles = new ArrayList<File>();
		for (String file : files) {
			File romFile = new File(file);
			if (!romFile.exists() || !romFile.canRead()) {
				System.err.println("Rom file description '" + romFile
						+ "' does not exist or is not readable");
				System.exit(1);
			} else {
				depRomFiles.add(romFile);
			}
		}
		return depRomFiles;
	}

	private static JsonObject getConfigContent(CommandLine line)
			throws FileNotFoundException {
		JsonObject configContents = null;
		String configValue = line.getOptionValue(CONFIG);
		if (configValue != null) {
			File configFile = new File(configValue);
			if (!configFile.exists() || !configFile.canRead()) {
				System.err.println("Config file '" + configFile
						+ "' does not exist or is not readable");
				System.exit(1);
			}
			configContents = loadConfigFile(configFile);
		}
		return configContents;
	}

	private static File getCodegenDir(CommandLine line) {
		File codegenDir = new File(line.getOptionValue(CODEGEN));
		if (codegenDir.exists()) {
			if (!codegenDir.canWrite()) {
				System.err.println("Codegen '" + codegenDir
						+ "' is not writable");
				System.exit(1);
			} else if (!codegenDir.isDirectory()) {
				System.err.println("Codegen '" + codegenDir
						+ "' is not a directory");
				System.exit(1);
			}
		}
		return codegenDir;
	}

	private static File getTemplatesDir(CommandLine line) {
		File templatesDir = new File(line.getOptionValue(TEMPLATES));

		if (templatesDir.exists()) {
			if (!templatesDir.canRead()) {
				System.err.println("TemplatesDir '" + templatesDir
						+ "' is not readable");
				System.exit(1);
			} else if (!templatesDir.isDirectory()) {
				System.err.println("TemplatesDir '" + templatesDir
						+ "' is not a directory");
				System.exit(1);
			}
		}
		return templatesDir;
	}

	private static File getRomFile(CommandLine line) {
		File romFile = new File(line.getOptionValue(ROM));
		if (!romFile.exists() || !romFile.canRead()) {
			System.err.println("Rom file description '" + romFile
					+ "' does not exist or is not readable");
			System.exit(1);
		}
		return romFile;
	}

	private static JsonObject loadConfigFile(File configFile)
			throws JsonIOException, FileNotFoundException {
		GsonBuilder gsonBuilder = new GsonBuilder();
		Gson gson = gsonBuilder.create();
		try {
			JsonElement element = gson.fromJson(new FileReader(configFile),
					JsonElement.class);
			return element.getAsJsonObject();
		} catch (JsonSyntaxException e) {
			System.err.println("Config file '" + configFile
					+ "' has the following formatting error:");
			System.err.println(e.getLocalizedMessage());
			System.exit(1);
			return null;
		}
	}

	private static void deleteIfNecessary(boolean delete,
			JsonObject configContent, File codegenDir) throws IOException {

		if (delete && codegenDir.exists()) {

			List<String> noDeleteFiles = new ArrayList<String>();
			if (configContent != null) {
				JsonArray array = configContent.getAsJsonArray("no_delete");
				if (array != null) {
					for (JsonElement elem : array) {
						if (elem instanceof JsonPrimitive) {
							noDeleteFiles.add(((JsonPrimitive) elem)
									.getAsString());
						}
					}
				}
			}

			delete(codegenDir, noDeleteFiles);
		}
	}

	public static void delete(File f, List<String> noDeleteFiles)
			throws IOException {
		delete(f, f, noDeleteFiles);
	}

	public static void delete(File basePath, File f, List<String> noDeleteFiles)
			throws IOException {

		Path relativePath = basePath.toPath().relativize(f.toPath());

		if (noDeleteFiles.contains(relativePath.toString())) {
			return;
		}

		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(basePath, c, noDeleteFiles);
			}
		}

		if (f.listFiles() == null || f.listFiles().length == 0) {
			if (!f.delete()) {
				throw new FileNotFoundException("Failed to delete file: " + f);
			}
		}
	}

	public static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ktool-rom-processor", options);
	}
}
