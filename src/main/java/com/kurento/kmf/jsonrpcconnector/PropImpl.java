package com.kurento.kmf.jsonrpcconnector;

public class PropImpl implements Prop {

	private String name;
	private Object value;

	public PropImpl(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getValue() {
		return value;
	}

}
