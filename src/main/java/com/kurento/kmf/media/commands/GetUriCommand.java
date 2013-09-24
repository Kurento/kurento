package com.kurento.kmf.media.commands;

import com.kurento.kmf.media.IsMediaCommand;

@IsMediaCommand(type = GetUriCommand.TYPE)
public class GetUriCommand extends MediaCommand {

	public static final String TYPE = "GetUriCommand";

	public GetUriCommand() {
		super(TYPE);
	}

	@Override
	public byte[] getData() {
		return new byte[0];
	}

}
