package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.commands.internal.AbstractMediaCommand;
import com.kurento.kmf.media.commands.internal.VoidCommandResult;

@ProvidesMediaCommand(type = VoidContinuation.TYPE, resultClass = VoidCommandResult.class)
public class VoidContinuation extends AbstractMediaCommand {

	public static final String TYPE = "VoidContinuation";

	public VoidContinuation(String name) {
		super(name);
	}

	@Override
	public byte[] getData() {
		return null;
	}
}
