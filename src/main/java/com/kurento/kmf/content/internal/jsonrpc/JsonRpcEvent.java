package com.kurento.kmf.content.internal.jsonrpc;

public class JsonRpcEvent {
	private String type;
	private String data;

	public static JsonRpcEvent newEvent(String type, String data) {
		return new JsonRpcEvent(type, data);
	}

	JsonRpcEvent() {
	}

	JsonRpcEvent(String type, String data) {
		this.type = type;
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public String getData() {
		return data;
	}
}
