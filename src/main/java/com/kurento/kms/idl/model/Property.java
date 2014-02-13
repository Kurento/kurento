package com.kurento.kms.idl.model;

public class Property extends DataItem {

	public Property(String name, Doc doc, TypeRef type, boolean required) {
		super(name, doc, type, required);
	}

	@Override
	public String toString() {
		return "Property [type()=" + getType() + ", required="
				+ isOptional() + ", doc=" + getDoc() + ", name="
				+ getName() + "]";
	}

	
}
