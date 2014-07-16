package com.kurento.ktool.rom.processor.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.kurento.ktool.rom.processor.model.Model;

public class ModelManager {

	private final Map<String, Model> models;
	private ModelManager dependencies;

	public ModelManager() {
		this.models = new HashMap<>();
	}

	public void addModel(Model model) {
		String name = model.getName();
		Model prevModel = this.models.get(name);
		if (prevModel != null) {

			if (!prevModel.getVersion().equals(model.getVersion())) {
				throw new KurentoRomProcessorException(
						"Error: Found plugin '"
								+ name
								+ "' with different versions in dependencies. Kurento "
								+ "Rom Processor doesn't allow several versions for the same plugin at the same time");
			}

			prevModel.fusionModel(model);
		} else {
			this.models.put(name, model);
		}
	}

	public void resolveModels() {
		for (Model model : models.values()) {
			model.resolveModel(this);
		}
	}

	public Model getModel(String name, String version) {
		Model model = models.get(name);
		if (model != null) {
			if (model.getVersion().equals(version)) {
				return model;
			}
		} else {
			if (dependencies != null) {
				return dependencies.getModel(name, version);
			}
		}

		return null;
	}

	private void removeModel(String name) {
		Model model = models.get(name);
		if (model != null) {
			models.remove(model.getName());
		}

		if (dependencies != null) {
			dependencies.removeModel(name);
		}
	}

	public void setDependencies(ModelManager dependencies) {
		this.dependencies = dependencies;

		for (Model model : dependencies.getModels()) {
			if (models.get(model.getName()) != null) {
				this.dependencies.removeModel(model.getName());
			}
		}
	}

	Collection<Model> getModels() {
		return models.values();
	}
}
