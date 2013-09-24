package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.internal.VoidCommand;
import com.kurento.kmf.media.internal.VoidContinuationWrapper;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "RecorderEndPoint")
public class RecorderEndPoint extends UriEndPoint {

	public static final String TYPE = "RecorderEndPoint";

	RecorderEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	public void record() {
		MediaCommand command = new VoidCommand("record");
		sendCommand(command);
	}

	/* ASYNC */
	public void record(final Continuation<Void> cont) {
		MediaCommand command = new VoidCommand("record");
		VoidContinuationWrapper wrappedCont = new VoidContinuationWrapper(cont);
		sendCommand(command, wrappedCont);
	}
}
