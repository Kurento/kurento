package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.SET_URI, resultClass = VoidCommandResult.class)
public class SetUriCommand extends StringCommand {

	public SetUriCommand(String uri) {
		super(mediaCommandDataTypesConstants.SET_URI, uri);
	}

}
