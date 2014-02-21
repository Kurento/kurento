package com.kurento.kms.idl.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Event extends Type {

	private List<Property> properties;

	@SerializedName("extends")
	private TypeRef extendsProp;

	public Event(String name, Doc doc, List<Property> properties) {
		super(name, doc);
		this.properties = properties;
	}

	public void setExtends(TypeRef extendsProp) {
		this.extendsProp = extendsProp;
	}

	public TypeRef getExtends() {
		return extendsProp;
	}

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((extendsProp == null) ? 0 : extendsProp.hashCode());
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		if (extendsProp == null) {
			if (other.extendsProp != null)
				return false;
		} else if (!extendsProp.equals(other.extendsProp))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Event [properties=" + properties + ", extendsProp="
				+ extendsProp + ", getDoc()=" + getDoc() + ", getName()="
				+ getName() + "]";
	}

	@Override
	public List<ModelElement> getChildren() {
		return new ArrayList<ModelElement>(properties);
	}

}
