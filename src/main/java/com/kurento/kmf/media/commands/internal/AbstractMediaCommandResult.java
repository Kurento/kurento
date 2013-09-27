package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.commands.MediaCommandResult;
import com.kurento.kms.thrift.api.CommandResult;

public abstract class AbstractMediaCommandResult implements MediaCommandResult {

	private final String dataType;

	public AbstractMediaCommandResult(String dataType) {
		this.dataType = dataType;
	}

	@Override
	public String getDataType() {
		return dataType;
	}

	public abstract void deserializeCommandResult(CommandResult result);

}
