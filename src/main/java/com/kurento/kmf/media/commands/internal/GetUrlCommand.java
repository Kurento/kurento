package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.GET_URL, resultClass = StringCommandResult.class)
public class GetUrlCommand extends VoidCommand {

	public GetUrlCommand() {
		super(mediaCommandDataTypesConstants.GET_URL);
	}

}
