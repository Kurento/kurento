package com.kurento.modulecreator.definition;

public class Import {

	private String name;
	private String version;
	private transient ModuleDefinition module;

	public Import(String name, String version) {
		super();
		this.name = name;
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public void setModule(ModuleDefinition module) {
		this.module = module;
	}

	public ModuleDefinition getModule() {
		return module;
	}

	@Override
	public String toString() {
		return name + "(" + version + ")";
	}
}