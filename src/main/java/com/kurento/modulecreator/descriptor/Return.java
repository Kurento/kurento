package com.kurento.modulecreator.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Return extends ModelElement {

	private TypeRef type;
	private String doc;

	public Return(TypeRef type, String doc) {
		super();
		this.type = type;
		this.doc = doc;
	}

	public String getDoc() {
		return doc;
	}

	public TypeRef getType() {
		return type;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	public void setType(TypeRef type) {
		this.type = type;
	}

	@Override
	public List<ModelElement> getChildren() {
		return new ArrayList<ModelElement>(Arrays.asList(type));
	}

	@Override
	public String toString() {
		return "Return [type=" + type + ", doc=" + doc + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((doc == null) ? 0 : doc.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Return other = (Return) obj;
		if (doc == null) {
			if (other.doc != null)
				return false;
		} else if (!doc.equals(other.doc))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}
