package com.kurento.ktool.rom.processor.model;

public class Import {

	private String name;
	private String version;
	private transient Model model;

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return model;
	}

	@Override
	public String toString() {
		return name + "(" + version + ")";
	}
}