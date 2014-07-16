package com.kurento.ktool.rom.processor.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kurento.ktool.rom.processor.model.Model;

public class ModelManager {

	private final Map<String, Model> models;
	private ModelManager dependencies;

	public ModelManager() {
		this.models = new HashMap<>();
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

	public Collection<Model> getModels() {
		return models.values();
	}

	public void addModels(List<Model> models) {
		for (Model model : models) {
			model.validateModel();
			addModel(model);
		}
	}

	public void addModel(Model model) {
		this.models.put(model.getName(), model);
	}

	public void addModelInSeveralKmdFiles(List<Model> models) {
		Model model = models.get(0);
		for (int i = 1; i < models.size(); i++) {
			model.fusionModel(models.get(i));
		}
		addModel(model);
	}
}
