package com.kurento.ktool.rom.processor.codegen;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kurento.ktool.rom.processor.json.JsonModelSaverLoader;
import com.kurento.ktool.rom.processor.model.Model;

public class KurentoRomProcessor {

	private Path codegenDir;
	private JsonObject config = new JsonObject();
	private Path templatesDir;
	private boolean verbose;
	private boolean deleteGenDir;
	private List<Path> dependencyKmdFiles = new ArrayList<Path>();
	private List<Path> kmdFiles = new ArrayList<Path>();
	private boolean listGeneratedFiles = false;

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

	public Result generateCode() {

		try {

			deleteIfNecessary(deleteGenDir, config, codegenDir);

			List<Model> dependencyModels = new ArrayList<Model>();
			for (Path dependencyRomFile : dependencyKmdFiles) {
				Model depModel = JsonModelSaverLoader.getInstance()
						.loadFromFile(dependencyRomFile);
				depModel.populateModel();
				dependencyModels.add(depModel);
			}

			Model model = new Model();
			for (Path kmdFile : kmdFiles) {
				model.addElements(JsonModelSaverLoader.getInstance()
						.loadFromFile(kmdFile));
			}

			model.populateModel(dependencyModels);

			CodeGen codeGen = new CodeGen(templatesDir, codegenDir, verbose,
					listGeneratedFiles, config);

			if (config.has("expandMethodsWithOpsParams")
					&& config.get("expandMethodsWithOpsParams").getAsBoolean()) {
				model.expandMethodsWithOpsParams();
			}

			codeGen.generateCode(model);

			return new Result();

		} catch (Exception e) {

			e.printStackTrace();
			return new Result(new Error(e.getClass().getName() + ": "
					+ e.getMessage()));

		}
	}

	private static void deleteIfNecessary(boolean delete,
			JsonObject configContent, Path codegenDir) throws IOException {

		if (delete && Files.exists(codegenDir)) {

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

	public static void delete(Path f, List<String> noDeleteFiles)
			throws IOException {
		delete(f, f, noDeleteFiles);
	}

	public static void delete(Path basePath, Path f, List<String> noDeleteFiles)
			throws IOException {

		Path relativePath = basePath.relativize(f);

		if (noDeleteFiles.contains(relativePath.toString())) {
			return;
		}

		if (Files.isDirectory(f)) {

			try (DirectoryStream<Path> directoryStream = Files
					.newDirectoryStream(f)) {
				for (Path c : directoryStream) {
					delete(basePath, c, noDeleteFiles);
				}
			}

			if (emptyDir(f)) {
				System.out.println("Deleting folder: " + f);
				Files.delete(f);
			}
		} else {
			System.out.println("Deleting file: " + f);
			Files.delete(f);
		}
	}

	private static boolean emptyDir(Path path) throws IOException {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
			Iterator<Path> files = ds.iterator();
			return !files.hasNext();
		}
	}

	public void setListGeneratedFiles(boolean listGeneratedFiles) {
		this.listGeneratedFiles = listGeneratedFiles;
	}
}
