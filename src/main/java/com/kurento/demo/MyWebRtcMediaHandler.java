package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.WebRtcMediaRequest;
import com.kurento.kmf.content.WebRtcMediaService;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcEvent;
import com.kurento.kmf.content.internal.webrtc.WebRtcMediaRequestImpl;

@WebRtcMediaService(name = "WebRtcMediaHandler", path = "/webrtc")
public class MyWebRtcMediaHandler implements WebRtcMediaHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyWebRtcMediaHandler.class);

	@Override
	public void onMediaRequest(WebRtcMediaRequest request)
			throws ContentException {
		log.debug("onMediaRequest");
		request.startMedia(null, null);
		((WebRtcMediaRequestImpl) request).produceEvents(JsonRpcEvent.newEvent(
				"test-event-type", "test-event-data"));
	}

	@Override
	public void onMediaTerminated(String requestId) {
		log.debug("onMediaTerminated");
	}

	@Override
	public void onMediaError(String requestId, ContentException exception) {
		log.debug("onMediaError");
	}
}