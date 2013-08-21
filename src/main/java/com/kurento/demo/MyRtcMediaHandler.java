package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.WebRtcMediaRequest;
import com.kurento.kmf.content.WebRtcMediaService;
import com.kurento.kmf.content.internal.WebRtcMediaRequestImpl;
import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonEvent;

@WebRtcMediaService(name = "WebRtcMediaHandler", path = "/webrtc")
public class MyRtcMediaHandler implements WebRtcMediaHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyRtcMediaHandler.class);

	@Override
	public void onMediaRequest(WebRtcMediaRequest request)
			throws ContentException {
		log.info("onMediaRequest");
		request.startMedia(null, null);
		((WebRtcMediaRequestImpl) request).produceEvents(WebRtcJsonEvent
				.newEvent("test-event-type", "test-event-data"));
	}

	@Override
	public void onMediaTerminated(String requestId) {
		log.info("onMediaTerminated");
		// TODO Auto-generated method stub
	}

	@Override
	public void onMediaError(String requestId, ContentException exception) {
		log.info("onMediaError");
		// TODO Auto-generated method stub
	}
}
