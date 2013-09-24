package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.IsMediaCommand;
import com.kurento.kmf.media.commands.MediaCommand;

@IsMediaCommand(type = StringCommand.TYPE)
public class StringCommand extends MediaCommand {

	public static final String TYPE = "StringCommand";

	public StringCommand(String name) {
		super(name);
	}

	@Override
	public byte[] getData() {
		// TODO Store the data and convert it to a byte[] to be serialized by
		// thrift
		return null;
	}
}
