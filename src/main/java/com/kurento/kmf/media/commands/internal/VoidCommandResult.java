package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommandResult;
import com.kurento.kms.thrift.api.CommandResult;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommandResult(dataType = mediaCommandDataTypesConstants.VOID_COMMAND_RESULT)
public class VoidCommandResult extends AbstractMediaCommandResult {

	VoidCommandResult() {
		super(mediaCommandDataTypesConstants.VOID_COMMAND_RESULT);
	}

	@Override
	public void deserializeCommandResult(CommandResult result) {
	}

}
