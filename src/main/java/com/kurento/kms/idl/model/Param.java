package com.kurento.kms.idl.model;

import com.google.gson.JsonElement;

public class Param extends DataItem {

	public Param(String name, Doc doc, TypeRef type) {
		super(name, doc, type);
	}

	public Param(String name, Doc doc, TypeRef type, JsonElement defaultValue) {
		super(name, doc, type, defaultValue);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Param [type()=" + getType() + ", required()=" + isOptional()
				+ ", doc()=" + getDoc());
		if (isOptional()) {
			sb.append(", defaultValue()=" + getDefaultValue() + "]");
		} else {
			sb.append("]");
		}

		return sb.toString();
	}
}
