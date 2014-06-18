package com.kurento.ktool.rom.processor.model;

import com.google.gson.JsonElement;

public class Property extends DataItem {

	private boolean readOnly = false;
	private boolean finalProp = false;

	public Property(String name, String doc, TypeRef type, boolean optional) {
		super(name, doc, type, optional);
	}

	public Property(String name, String doc, TypeRef type,
			JsonElement defaultValue) {
		super(name, doc, type, defaultValue);
	}

	public void setFinal(boolean finalProp) {
		this.finalProp = finalProp;
	}

	public boolean isFinal() {
		return finalProp;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Property [type()=");
		sb.append(getType());
		sb.append(", optional()=");
		sb.append(isOptional());
		sb.append(", doc()=");
		sb.append(getDoc());
		if (isOptional()) {
			sb.append(", defaultValue()=");
			sb.append(getDefaultValue());
		}
		sb.append("]");

		return sb.toString();
	}

}
