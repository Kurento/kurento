package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.STOP)
public class StartCommand extends VoidCommand {

	public StartCommand() {
		super(mediaCommandDataTypesConstants.STOP);
	}

}
