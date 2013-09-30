package com.kurento.kmf.media.commands.internal;

import com.kurento.kms.thrift.api.CommandResult;

public class VoidCommandResult extends AbstractMediaCommandResult {

	VoidCommandResult() {
		super();
	}

	@Override
	public void deserializeCommandResult(CommandResult result) {
	}

}
