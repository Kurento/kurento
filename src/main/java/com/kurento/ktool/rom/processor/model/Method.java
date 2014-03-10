package com.kurento.ktool.rom.processor.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Method extends NamedElement {

	private List<Param> params;

	@SerializedName("return")
	private Return returnProp;

	public Method(String name, String doc, List<Param> params, Return returnProp) {
		super(name, doc);
		this.params = params;
		this.returnProp = returnProp;
	}

	public List<Param> getParams() {
		return params;
	}

	public Return getReturn() {
		return returnProp;
	}

	public void setParams(List<Param> params) {
		this.params = params;
	}

	public void setReturnProp(Return returnProp) {
		this.returnProp = returnProp;
	}

	@Override
	public String toString() {
		return "Method [params=" + params + ", return=" + returnProp + ", doc="
				+ getDoc() + ", name=" + getName() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((params == null) ? 0 : params.hashCode());
		result = prime * result
				+ ((returnProp == null) ? 0 : returnProp.hashCode());
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
		Method other = (Method) obj;
		if (params == null) {
			if (other.params != null)
				return false;
		} else if (!params.equals(other.params))
			return false;
		if (returnProp == null) {
			if (other.returnProp != null)
				return false;
		} else if (!returnProp.equals(other.returnProp))
			return false;
		return true;
	}

	@Override
	public List<ModelElement> getChildren() {
		List<ModelElement> children = new ArrayList<ModelElement>(params);
		if (returnProp != null) {
			children.add(returnProp);
		}
		return children;
	}

}
