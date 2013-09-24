package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.IsMediaCommand;
import com.kurento.kmf.media.commands.MediaCommand;

@IsMediaCommand(type = VoidCommand.TYPE)
public class VoidCommand extends MediaCommand {

	public static final String TYPE = "VoidCommand";

	public VoidCommand(String name) {
		super(name);
	}

	@Override
	public byte[] getData() {
		return null;
	}
}
