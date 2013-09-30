package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.PAUSE, resultClass = VoidCommandResult.class)
public class PauseCommand extends VoidCommand {

	public PauseCommand() {
		super(mediaCommandDataTypesConstants.PAUSE);
	}

}
