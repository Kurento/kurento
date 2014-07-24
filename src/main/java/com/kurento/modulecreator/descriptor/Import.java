package com.kurento.modulecreator.descriptor;

public class Import {

	private String name;
	private String version;
	private transient ModuleDescriptor module;

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

	public void setModule(ModuleDescriptor module) {
		this.module = module;
	}

	public ModuleDescriptor getModule() {
		return module;
	}

	@Override
	public String toString() {
		return name + "(" + version + ")";
	}
}