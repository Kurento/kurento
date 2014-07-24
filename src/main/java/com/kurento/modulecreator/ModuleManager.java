package com.kurento.modulecreator;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kurento.modulecreator.definition.ModuleDefinition;

public class ModuleManager {

	private final Map<String, ModuleDefinition> modules;
	private ModuleManager dependencies;

	public ModuleManager() {
		this.modules = new HashMap<>();
	}

	public void resolveModules() {
		for (ModuleDefinition module : modules.values()) {
			module.resolveModule(this);
		}
	}

	public ModuleDefinition getModule(String name) {
		ModuleDefinition module = modules.get(name);
		if (module != null) {
			return module;
		} else {
			if (dependencies != null) {
				return dependencies.getModule(name);
			}
		}
		return null;
	}

	public ModuleDefinition getModule(String name, String version) {
		ModuleDefinition module = modules.get(name);
		if (module != null) {
			if (module.getVersion().equals(version)) {
				return module;
			}
		} else {
			if (dependencies != null) {
				return dependencies.getModule(name, version);
			}
		}

		return null;
	}

	private void removeModule(String name) {
		ModuleDefinition module = modules.get(name);
		if (module != null) {
			modules.remove(module.getName());
		}

		if (dependencies != null) {
			dependencies.removeModule(name);
		}
	}

	public void setDependencies(ModuleManager dependencies) {
		this.dependencies = dependencies;

		for (ModuleDefinition module : dependencies.getModules()) {
			if (modules.get(module.getName()) != null) {
				this.dependencies.removeModule(module.getName());
			}
		}
	}

	public Collection<ModuleDefinition> getModules() {
		return modules.values();
	}

	public void addModules(List<ModuleDefinition> modules) {
		for (ModuleDefinition module : modules) {
			module.validateModule();
			addModule(module);
		}
	}

	public void addModule(ModuleDefinition module) {
		this.modules.put(module.getName(), module);
	}

	public void addModuleInSeveralKmdFiles(List<ModuleDefinition> modules) {
		ModuleDefinition module = modules.get(0);
		for (int i = 1; i < modules.size(); i++) {
			module.fusionModules(modules.get(i));
		}
		addModule(module);
	}
}
