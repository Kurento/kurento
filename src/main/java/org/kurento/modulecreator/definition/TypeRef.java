package org.kurento.modulecreator.definition;

public class TypeRef extends ModelElement {

	private String name;
	private boolean isList;
	private transient Type type;

	public TypeRef(String name) {
		super();
		if (!name.endsWith("[]")) {
			this.name = name;
			this.isList = false;
		} else {
			this.name = name.substring(0, name.length() - 2);
			this.isList = true;
		}
	}

	public TypeRef(String name, boolean isList) {
		super();
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

	@Override
	public String toString() {
		return "TypeRef [name=" + name + ", isList=" + isList + "]";
	}

}
