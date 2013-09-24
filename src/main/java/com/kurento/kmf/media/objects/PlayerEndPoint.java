package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.internal.VoidCommand;
import com.kurento.kmf.media.internal.VoidContinuationWrapper;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "PlayerEndPoint")
public class PlayerEndPoint extends UriEndPoint {

	public static final String TYPE = "PlayerEndPoint";

	PlayerEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	public void play() {
		MediaCommand command = new VoidCommand("play");
		sendCommand(command);
	}

	/* ASYNC */
	public void play(final Continuation<Void> cont) {
		MediaCommand command = new VoidCommand("play");
		VoidContinuationWrapper wrappedCont = new VoidContinuationWrapper(cont);
		sendCommand(command, wrappedCont);
	}
}
