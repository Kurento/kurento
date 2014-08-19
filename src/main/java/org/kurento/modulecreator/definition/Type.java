package org.kurento.modulecreator.definition;

public abstract class Type extends NamedElement {

	protected transient ModuleDefinition module;

	public Type(String name, String doc) {
		super(name, doc);
	}

	public ModuleDefinition getModule() {
		return module;
	}

	public void setModule(ModuleDefinition module) {
		this.module = module;
	}

}
