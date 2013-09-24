package com.kurento.kmf.media.commands;

import com.kurento.kms.thrift.api.CommandResult;

public abstract class MediaCommandResult {

	private final String dataType;

	MediaCommandResult(String dataType) {
		this.dataType = dataType;
	}

	public String getDataType() {
		return dataType;
	}

	public abstract void deserializeCommandResult(CommandResult commandResult);

}
