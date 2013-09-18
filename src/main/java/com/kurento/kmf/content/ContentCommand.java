package com.kurento.kmf.content;

//TODO make javadoc
public class ContentCommand {
	private String type;
	private String data;

	public ContentCommand() {
	}

	public ContentCommand(String type, String data) {
		this.type = type;
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
