package org.kurento.modulecreator.definition;

public class Import {

	private String name;
	private String version;
	private String mavenVersion;
	private String npmVersion;

	private transient ModuleDefinition module;

	public Import(String name, String version) {
		super();
		this.name = name;
		this.version = version;
	}

	public Import(String name, String version, String mavenVersion,
			String npmVersion) {
		super();
		this.name = name;
		this.version = version;
		this.mavenVersion = mavenVersion;
		this.npmVersion = npmVersion;
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

	public String getMavenVersion() {
		return mavenVersion;
	}

	public String getNpmVersion() {
		return npmVersion;
	}

	public void setMavenVersion(String mavenVersion) {
		this.mavenVersion = mavenVersion;
	}

	public void setNpmVersion(String npmVersion) {
		this.npmVersion = npmVersion;
	}
}