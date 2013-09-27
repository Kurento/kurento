package com.kurento.kmf.media.commands.internal;

public class VoidCommand extends AbstractMediaCommand {

	public VoidCommand(String type) {
		super(type);
	}

	public byte[] getData() {
		return null;
	}
}
