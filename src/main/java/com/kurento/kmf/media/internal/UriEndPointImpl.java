package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.UriEndPoint;
import com.kurento.kmf.media.commands.internal.GetUriCommand;
import com.kurento.kmf.media.commands.internal.PauseCommand;
import com.kurento.kmf.media.commands.internal.StartCommand;
import com.kurento.kmf.media.commands.internal.StopCommand;
import com.kurento.kmf.media.commands.internal.StringCommandResult;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public abstract class UriEndPointImpl extends EndPointImpl implements
		UriEndPoint {

	UriEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public String getUri() {
		StringCommandResult result = (StringCommandResult) sendCommand(new GetUriCommand());
		return result.getResult();
	}

	void start() {
		sendCommand(new StartCommand());
	}

	@Override
	public void pause() {
		sendCommand(new PauseCommand());
	}

	@Override
	public void stop() {
		sendCommand(new StopCommand());
	}

	/* ASYNC */
	@Override
	public void getUri(final Continuation<String> cont) {
		sendCommand(new GetUriCommand(), new StringContinuationWrapper(cont));
	}

	void start(final Continuation<Void> cont) {
		sendCommand(new StartCommand(), new VoidContinuationWrapper(cont));
	}

	@Override
	public void pause(final Continuation<Void> cont) {
		sendCommand(new PauseCommand(), new VoidContinuationWrapper(cont));
	}

	@Override
	public void stop(final Continuation<Void> cont) {
		sendCommand(new StopCommand(), new VoidContinuationWrapper(cont));
	}
}
