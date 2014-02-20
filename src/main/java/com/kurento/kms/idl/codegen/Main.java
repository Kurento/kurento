package com.kurento.kms.idl.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.kurento.kms.idl.json.JsonModel;
import com.kurento.kms.idl.model.Model;

import freemarker.template.TemplateException;

public class Main {

	private static final String HELP = "h";
	private static final String VERBOSE = "v";
	private static final String ROM = "r";
	private static final String TEMPLATES = "t";
	private static final String CODEGEN = "c";
	private static final String DELETE = "d";

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

		File romFile = new File(line.getOptionValue(ROM));
		if (!romFile.exists() || !romFile.canRead()) {
			System.err.println("Rom file description '" + romFile
					+ "' does not exist or is not readable");
			System.exit(1);
		}

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

		if (line.hasOption(DELETE) && codegenDir.exists()) {
			delete(codegenDir);
		}

		Model model = new JsonModel().loadFromFile(romFile);

		CodeGen codeGen = new CodeGen(templatesDir, codegenDir,
				line.hasOption(VERBOSE));

		codeGen.generateCode(model);

		System.out.println("Generation complete");

	}

	public static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		if (!f.delete()) {
			throw new FileNotFoundException("Failed to delete file: " + f);
		}
	}

	public static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ktool-rom-processor", options);
	}
}
