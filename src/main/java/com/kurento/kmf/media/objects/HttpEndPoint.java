package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.commands.StringCommandResult;
import com.kurento.kmf.media.internal.StringContinuationWrapper;
import com.kurento.kmf.media.internal.VoidCommand;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = HttpEndPoint.TYPE)
public class HttpEndPoint extends EndPoint {

	public static final String TYPE = "HttpEndPoint";

	HttpEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	// TODO change return for URL type?
	public String getUrl() {
		MediaCommand command = new VoidCommand("getUrl");
		StringCommandResult result = (StringCommandResult) sendCommand(command);
		return result.getString();
	}

	/* ASYNC */

	public void getUrl(final Continuation<String> cont) {
		MediaCommand command = new VoidCommand("getUrl");
		StringContinuationWrapper wrappedCont = new StringContinuationWrapper(
				cont);
		sendCommand(command, wrappedCont);
	}
}
