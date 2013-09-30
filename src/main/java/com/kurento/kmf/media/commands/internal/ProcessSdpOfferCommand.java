package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.PROCESS_SDP_OFFER, resultClass = StringCommandResult.class)
public class ProcessSdpOfferCommand extends StringCommand {

	public ProcessSdpOfferCommand(String sdp) {
		super(mediaCommandDataTypesConstants.PROCESS_SDP_OFFER, sdp);
	}
}
