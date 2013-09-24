package com.kurento.kmf.media.commands;

import com.kurento.kmf.media.IsMediaCommandResult;
import com.kurento.kms.thrift.api.CommandResult;

@IsMediaCommandResult(dataType = VoidCommandResult.TYPE)
public class VoidCommandResult extends MediaCommandResult {

	public static final String TYPE = "VoidCommandResult";

	VoidCommandResult() {
		super(TYPE);
	}

	@Override
	public void deserializeCommandResult(CommandResult result) {
		// This is a void command, therefore no result is expected
	}

}
