package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.RtpMediaService;

@RtpMediaService(name = "simpleRtpMediaService", path = "/rtp")
public class RtpLoopBackMediaHandler implements RtpMediaHandler {

	@Override
	public void onMediaRequest(RtpMediaRequest request) throws ContentException {
		request.startMedia(null, null);
	}

	@Override
	public void onMediaTerminated(String requestId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMediaError(String requestId, ContentException exception) {
		// TODO Auto-generated method stub

	}

}
