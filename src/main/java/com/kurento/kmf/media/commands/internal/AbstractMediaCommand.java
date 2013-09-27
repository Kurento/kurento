package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kms.thrift.api.Command;

public abstract class AbstractMediaCommand implements MediaCommand {

	private final String type;

	protected AbstractMediaCommand(String type) {
		this.type = type;
	}

	@Override
	public String getType() {
		return this.type;
	}

	protected abstract byte[] getData();

	public Command getThriftCommand() {
		Command command = new Command();
		command.setType(getType());
		command.setData(getData());
		return command;
	}

}
