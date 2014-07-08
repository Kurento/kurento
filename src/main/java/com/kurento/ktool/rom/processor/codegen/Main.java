package com.kurento.ktool.rom.processor.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

import freemarker.template.TemplateException;

public class Main {

	private static final String HELP = "h";
	private static final String VERBOSE = "v";
	private static final String LIST_GEN_FILES = "lf";
	private static final String ROM = "r";
	private static final String DEPROM = "dr";
	private static final String TEMPLATES_DIR = "t";
	private static final String CODEGEN = "c";
	private static final String DELETE = "d";
	private static final String CONFIG = "cf";
	private static final String INTERNAL_TEMPLATES = "it";

	public static void main(String[] args) throws IOException,
			TemplateException {

		Options options = configureOptions();

		CommandLine line = null;

		try {

			CommandLineParser parser = new PosixParser();
			line = parser.parse(options, args);

			if (line.hasOption(HELP) || !line.hasOption(ROM)
					|| !line.hasOption(CODEGEN)) {
				printHelp(options);
				System.exit(0);
			}
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			printHelp(options);
			System.exit(1);
		}

		KurentoRomProcessor krp = new KurentoRomProcessor();
		krp.setDeleteGenDir(line.hasOption(DELETE));
		krp.setVerbose(line.hasOption(VERBOSE));
		krp.setListGeneratedFiles(line.hasOption(LIST_GEN_FILES));

		if (line.hasOption(TEMPLATES_DIR)) {
			krp.setTemplatesDir(getTemplatesDir(line));
		} else if (line.hasOption(INTERNAL_TEMPLATES)) {
			krp.setInternalTemplates(line.getOptionValue(INTERNAL_TEMPLATES));
		} else {
			System.err.println("Templates dir must be specified with -"
					+ TEMPLATES_DIR + " option or with -" + INTERNAL_TEMPLATES
					+ " option.");
			printHelp(options);
			System.exit(1);
		}

		krp.setCodeGenDir(getCodegenDir(line));
		krp.setConfig(getConfigContent(line));
		krp.addKmdFile(getKmdFile(line));
		krp.setDependencyKmdFiles(getDependencyKmdFiles(line));

		Result result = krp.generateCode();

		if (result.isSuccess()) {
			System.out.println("Generation success");
		} else {
			System.out.println("Generation failed");
			result.showErrorsInConsole();
		}
	}

	@SuppressWarnings("static-access")
	private static Options configureOptions() {

		// create the Options
		Options options = new Options();
		options.addOption(VERBOSE, "verbose", false,
				"Prints source code while being generated.");

		options.addOption(HELP, "help", false, "Prints this message.");

		options.addOption(OptionBuilder
				.withLongOpt("rom")
				.withDescription(
						"Kurento Media Element Description (kmd) file.")
				.hasArg().withArgName("ROM_FILE").isRequired().create(ROM));

		options.addOption(OptionBuilder
				.withLongOpt("deprom")
				.withDescription(
						"Kurento Media Element Description files used as dependencies.")
				.hasArg().withArgName("DEP_ROM_FILE").create(DEPROM));

		options.addOption(OptionBuilder.withLongOpt("templates")
				.withDescription("Directory that contains template files.")
				.hasArg().withArgName("TEMPLATES_DIR").create(TEMPLATES_DIR));

		options.addOption(OptionBuilder.withLongOpt("templates")
				.withDescription("Directory that contains template files.")
				.hasArg().withArgName("TEMPLATES_DIR")
				.create(INTERNAL_TEMPLATES));

		options.addOption(OptionBuilder.withLongOpt("codegen")
				.withDescription("Destination directory for generated files.")
				.hasArg().withArgName("CODEGEN_DIR").isRequired()
				.create(CODEGEN));

		options.addOption(DELETE, "delete", false,
				"Delete destination directory before generating files.");

		options.addOption(LIST_GEN_FILES, "list-generated-files", false,
				"List in the standard output the names of generated files.");

		options.addOption(OptionBuilder.withLongOpt("config")
				.withDescription("Configuration file.").hasArg()
				.withArgName("CONFIGURATION_FILE").create(CONFIG));
		return options;
	}

	public static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ktool-rom-processor", options);
	}

	private static List<Path> getDependencyKmdFiles(CommandLine line) {
		String[] files = line.getOptionValue(DEPROM).split(",");
		List<Path> depKmdFiles = new ArrayList<Path>();
		for (String file : files) {
			File romFile = new File(file);
			if (!romFile.exists() || !romFile.canRead()) {
				System.err.println("Rom file description '" + romFile
						+ "' does not exist or is not readable");
				System.exit(1);
			} else {
				depKmdFiles.add(romFile.toPath());
			}
		}
		return depKmdFiles;
	}

	private static JsonObject getConfigContent(CommandLine line)
			throws JsonIOException, IOException {

		JsonObject configContents = new JsonObject();

		String configValue = line.getOptionValue(CONFIG);
		if (configValue != null) {
			Path configFile = Paths.get(configValue);
			if (!Files.exists(configFile)) {
				System.err.println("Config file '" + configFile
						+ "' does not exist or is not readable");
				System.exit(1);
			}
			configContents = KurentoRomProcessor.loadConfigFile(configFile);
		}

		return configContents;
	}

	private static Path getCodegenDir(CommandLine line) {
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
		return codegenDir.toPath();
	}

	private static Path getTemplatesDir(CommandLine line) {
		File templatesDir = new File(line.getOptionValue(TEMPLATES_DIR));

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

			return templatesDir.toPath();

		} else {

			System.err.println("TemplatesDir '" + templatesDir
					+ "' doesn't exist");
			System.exit(1);
			return null;
		}
	}

	private static Path getKmdFile(CommandLine line) {
		File romFile = new File(line.getOptionValue(ROM));
		if (!romFile.exists() || !romFile.canRead()) {
			System.err.println("Rom file description '" + romFile
					+ "' does not exist or is not readable");
			System.exit(1);
		}
		return romFile.toPath();
	}

}
