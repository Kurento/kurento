package com.kurento.ktool.rom.processor.codegen;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class KurentoRomProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoRomProcessor.class);

	private static final String CONFIG_FILE_NAME = "config.json";

	private Path codegenDir;
	private JsonObject config = new JsonObject();
	private Path templatesDir;
	private boolean verbose;
	private boolean deleteGenDir;
	private List<Path> dependencyKmdFiles = new ArrayList<Path>();
	private List<Path> kmdFiles = new ArrayList<Path>();
	private boolean listGeneratedFiles = false;
	private String internalTemplates = null;

	public void setInternalTemplates(String internalTemplates) {
		this.internalTemplates = internalTemplates;
	}

	public String getInternalTemplates() {
		return internalTemplates;
	}

	public void setKmdFiles(List<Path> kmdFiles) {
		this.kmdFiles = kmdFiles;
	}

	public void addKmdFile(Path kmdFile) {
		this.kmdFiles.add(kmdFile);
	}

	public void setConfig(JsonObject config) {
		this.config = config;
	}

	public void setCodeGenDir(Path codegenDir) {
		this.codegenDir = codegenDir;
	}

	public void setTemplatesDir(Path templatesDir) {
		this.templatesDir = templatesDir;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setDeleteGenDir(boolean deleteGenDir) {
		this.deleteGenDir = deleteGenDir;
	}

	public void setDependencyKmdFiles(List<Path> dependencyKmdFiles) {
		this.dependencyKmdFiles = dependencyKmdFiles;
	}

	public void addDependencyKmdFile(Path dependencyKmdFile) {
		this.dependencyKmdFiles.add(dependencyKmdFile);
	}

	public void setListGeneratedFiles(boolean listGeneratedFiles) {
		this.listGeneratedFiles = listGeneratedFiles;
	}

	private Path getInternalTemplatesDir(String internalTemplates)
			throws IOException {

		URL internalTemplatesAsURL = this.getClass().getResource(
				"/" + internalTemplates);

		if (internalTemplatesAsURL != null) {

			try {
				return PathUtils.getPathInClasspath(internalTemplatesAsURL);

			} catch (URISyntaxException e) {
				throw new KurentoRomProcessorException(
						"Error trying to load internal templates folder '"
								+ internalTemplates + "'", e);
			}

		} else {
			throw new KurentoRomProcessorException(
					"The internal templates folder '" + internalTemplates
							+ "' doesn't exist");
		}
	}

	public Result generateCode() throws JsonIOException, IOException {

		if (internalTemplates != null) {
			templatesDir = getInternalTemplatesDir(internalTemplates);

			Path configFile = templatesDir.resolve(CONFIG_FILE_NAME);

			if (Files.exists(configFile)) {
				JsonObject internalConfig = loadConfigFile(configFile);
				overrideConfig(internalConfig, config);
				config = internalConfig;
			}
		}

		try {

			if (deleteGenDir) {
				PathUtils.delete(codegenDir, loadNoDeleteFiles(config));
			}

			CodeGen codeGen = new CodeGen(templatesDir, codegenDir, verbose,
					listGeneratedFiles, config);

			ModelManager modelManager = createModelManager();

			for (Model model : modelManager.getModels()) {
				if (config.has("expandMethodsWithOpsParams")
						&& config.get("expandMethodsWithOpsParams")
								.getAsBoolean()) {
					model.expandMethodsWithOpsParams();
				}
				codeGen.generateCode(model);
			}

			return new Result();

		} catch (KurentoRomProcessorException e) {
			return new Result(new Error("Error: " + e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(new Error("Unexpected error: "
					+ e.getClass().getName() + " " + e.getMessage()));
		}
	}

	private List<String> loadNoDeleteFiles(JsonObject configContent) {
		List<String> noDeleteFiles = new ArrayList<String>();
		if (configContent != null) {
			JsonArray array = configContent.getAsJsonArray("no_delete");
			if (array != null) {
				for (JsonElement elem : array) {
					if (elem instanceof JsonPrimitive) {
						noDeleteFiles.add(((JsonPrimitive) elem).getAsString());
					}
				}
			}
		}
		return noDeleteFiles;
	}

	private ModelManager createModelManager() throws FileNotFoundException,
			IOException {

		log.info("Loading dependencies");
		ModelManager depModelManager = loadModelsFromPaths(dependencyKmdFiles);
		depModelManager.resolveModels();

		log.info("Loading kmd files to generate code");
		ModelManager modelManager = loadModelsFromPaths(kmdFiles);
		modelManager.setDependencies(depModelManager);
		modelManager.resolveModels();

		return modelManager;
	}

	private ModelManager loadModelsFromPaths(List<Path> kmdFiles)
			throws FileNotFoundException, IOException {

		ModelManager manager = new ModelManager();
		for (Path kmdFile : kmdFiles) {

			log.info("Loading kmdFile " + kmdFile);

			Model model = JsonModelSaverLoader.getInstance().loadFromFile(
					kmdFile);
			model.validateModel(kmdFile);
			manager.addModel(model);
		}
		return manager;
	}

	public static JsonObject loadConfigFile(Path configFile)
			throws JsonIOException, IOException {

		GsonBuilder gsonBuilder = new GsonBuilder();
		Gson gson = gsonBuilder.create();
		try {
			JsonElement element = gson.fromJson(
					Files.newBufferedReader(configFile,
							Charset.forName("UTF-8")), JsonElement.class);
			return element.getAsJsonObject();

		} catch (JsonSyntaxException e) {
			throw new KurentoRomProcessorException("Config file '" + configFile
					+ "' has the following formatting error:"
					+ e.getLocalizedMessage());
		}
	}

	private static void overrideConfig(JsonObject configContents,
			JsonObject newConfigContents) {

		for (Entry<String, JsonElement> e : newConfigContents.entrySet()) {
			configContents.add(e.getKey(), e.getValue());
		}
	}
}
