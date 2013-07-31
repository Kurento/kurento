package com.kurento.kmf.content;

public interface WebRtcMediaHandler {

	void onMediaRequest(WebRtcMediaRequest request) throws ContentException;

	void onMediaTerminated(String requestId);

	void onMediaError(String requestId, ContentException exception);
}
