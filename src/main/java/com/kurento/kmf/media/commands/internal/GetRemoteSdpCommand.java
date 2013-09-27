package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.GET_REMOTE_SDP)
public class GetRemoteSdpCommand extends VoidCommand {

	public GetRemoteSdpCommand() {
		super(mediaCommandDataTypesConstants.GET_REMOTE_SDP);
	}
}
