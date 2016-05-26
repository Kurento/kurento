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

  /**
   * Get qualified name mixing In the format module.name
   *
   * @return The qualified name
   */
  public String getQualifiedName() {
    if (this.module == null || this.module.getName() == null) {
      return name;
    }
    if (this.module.getName().equals("core") || this.module.getName().equals("filters")
        || this.module.getName().equals("elements")) {
      return "kurento." + name;
    } else {
      return this.module.getName() + "." + name;
    }
  }

}
