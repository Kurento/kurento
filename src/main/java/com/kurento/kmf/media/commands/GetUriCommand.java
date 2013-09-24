package com.kurento.kmf.media.commands;

public class GetUriCommand extends MediaCommand {

	public GetUriCommand() {
		super("GetUriCommand");
	}

	@Override
	public byte[] getData() {
		return new byte[0];
	}

}
