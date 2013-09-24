package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.commands.StringCommandResult;
import com.kurento.kmf.media.internal.StringContinuationWrapper;
import com.kurento.kmf.media.internal.VoidCommand;
import com.kurento.kmf.media.internal.VoidContinuationWrapper;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public abstract class UriEndPoint extends EndPoint {

	UriEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	// TODO change return for URI type?
	public String getUri() {
		MediaCommand command = new VoidCommand("getUri");
		StringCommandResult result = (StringCommandResult) sendCommand(command);
		return result.getString();
	}

	protected void start() {
		MediaCommand command = new VoidCommand("start");
		sendCommand(command);
	}

	public void pause() {
		MediaCommand command = new VoidCommand("pause");
		sendCommand(command);
	}

	public void stop() {
		MediaCommand command = new VoidCommand("stop");
		sendCommand(command);
	}

	/* ASYNC */
	public void getUri(final Continuation<String> cont) {
		MediaCommand command = new VoidCommand("getUri");
		StringContinuationWrapper wrappedCont = new StringContinuationWrapper(
				cont);
		sendCommand(command, wrappedCont);
	}

	protected void start(final Continuation<Void> cont) {
		MediaCommand command = new VoidCommand("start");
		VoidContinuationWrapper wrappedCont = new VoidContinuationWrapper(cont);
		sendCommand(command, wrappedCont);
	}

	public void pause(final Continuation<Void> cont) {
		MediaCommand command = new VoidCommand("pause");
		VoidContinuationWrapper wrappedCont = new VoidContinuationWrapper(cont);
		sendCommand(command, wrappedCont);
	}

	public void stop(final Continuation<Void> cont) {
		MediaCommand command = new VoidCommand("stop");
		VoidContinuationWrapper wrappedCont = new VoidContinuationWrapper(cont);
		sendCommand(command, wrappedCont);
	}
}
