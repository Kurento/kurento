package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.GET_URI)
public class GetUriCommand extends VoidCommand {

	public GetUriCommand() {
		super(mediaCommandDataTypesConstants.GET_URI);
	}

}
