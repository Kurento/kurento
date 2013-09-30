package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommand(type = mediaCommandDataTypesConstants.PROCESS_SDP_ANSWER, resultClass = StringCommandResult.class)
public class ProcessSdpAnswerCommand extends StringCommand {

	public ProcessSdpAnswerCommand(String sdp) {
		super(mediaCommandDataTypesConstants.PROCESS_SDP_ANSWER, sdp);
	}
}
