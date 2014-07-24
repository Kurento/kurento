package com.kurento.modulecreator.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kurento.modulecreator.descriptor.ModuleDescriptor;

public class ModuleManager {

	private final Map<String, ModuleDescriptor> modules;
	private ModuleManager dependencies;

	public ModuleManager() {
		this.modules = new HashMap<>();
	}

	public void resolveModules() {
		for (ModuleDescriptor module : modules.values()) {
			module.resolveModule(this);
		}
	}

	public ModuleDescriptor getModule(String name, String version) {
		ModuleDescriptor module = modules.get(name);
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
		ModuleDescriptor module = modules.get(name);
		if (module != null) {
			modules.remove(module.getName());
		}

		if (dependencies != null) {
			dependencies.removeModule(name);
		}
	}

	public void setDependencies(ModuleManager dependencies) {
		this.dependencies = dependencies;

		for (ModuleDescriptor module : dependencies.getModules()) {
			if (modules.get(module.getName()) != null) {
				this.dependencies.removeModule(module.getName());
			}
		}
	}

	public Collection<ModuleDescriptor> getModules() {
		return modules.values();
	}

	public void addModules(List<ModuleDescriptor> modules) {
		for (ModuleDescriptor module : modules) {
			module.validateModule();
			addModule(module);
		}
	}

	public void addModule(ModuleDescriptor module) {
		this.modules.put(module.getName(), module);
	}

	public void addModuleInSeveralKmdFiles(List<ModuleDescriptor> modules) {
		ModuleDescriptor module = modules.get(0);
		for (int i = 1; i < modules.size(); i++) {
			module.fusionModules(modules.get(i));
		}
		addModule(module);
	}
}
