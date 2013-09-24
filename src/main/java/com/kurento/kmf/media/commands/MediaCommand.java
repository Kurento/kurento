package com.kurento.kmf.media.commands;

public abstract class MediaCommand {

	private final String type;

	public MediaCommand(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	// TODO: should not be visible to final developer
	public abstract byte[] getData();

}
