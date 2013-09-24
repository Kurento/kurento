package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.IsMediaCommand;
import com.kurento.kmf.media.commands.MediaCommand;

@IsMediaCommand(type = VoidContinuation.TYPE)
public class VoidContinuation extends MediaCommand {

	public static final String TYPE = "VoidContinuation";

	public VoidContinuation(String name) {
		super(name);
	}

	@Override
	public byte[] getData() {
		return null;
	}
}
