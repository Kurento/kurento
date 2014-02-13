package com.kurento.kms.idl.model;

public class Param extends DataItem {

	public Param(String name, Doc doc, TypeRef type, boolean required) {
		super(name, doc, type, required);
	}

	@Override
	public String toString() {
		return "Param [type()=" + getType() + ", required()="
				+ isOptional() + ", doc()=" + getDoc() + ", name()="
				+ getName() + "]";
	}
	
	

}
