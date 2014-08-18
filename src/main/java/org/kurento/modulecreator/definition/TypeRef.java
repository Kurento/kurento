package org.kurento.modulecreator.definition;

import org.kurento.modulecreator.KurentoModuleCreatorException;

public class TypeRef extends ModelElement {

	private String name;
	private boolean isList;
	private transient String moduleName;

	private transient Type type;

	public static TypeRef parseFromJson(String typeRefString) {

		String moduleName = null;
		String name;
		boolean isList;

		if (!typeRefString.endsWith("[]")) {
			name = typeRefString;
			isList = false;
		} else {
			name = typeRefString.substring(0, typeRefString.length() - 2);
			isList = true;
		}

		String[] parts = name.split("\\.");
		if (parts.length == 2) {
			moduleName = parts[0];
			name = parts[1];
		} else if (parts.length > 2) {
			throw new KurentoModuleCreatorException(
					"Invalid module name in type ref: '" + name + "'");
		}

		return new TypeRef(moduleName, name, isList);
	}

	public TypeRef(String moduleName, String name, boolean isList) {
		super();
		this.moduleName = moduleName;
		this.name = name;
		this.isList = isList;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeRef other = (TypeRef) obj;
		if (isList != other.isList)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isList ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean isList() {
		return isList;
	}

	public void setList(boolean isList) {
		this.isList = isList;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public ModuleDefinition getModule() {
		return type.getModule();
	}

	public String getQualifiedName() {
		return (moduleName != null ? moduleName + "." : "") + name;
	}

	@Override
	public String toString() {
		return "TypeRef [name=" + name + ", isList=" + isList + ", moduleName="
				+ moduleName + "]";
	}

}
