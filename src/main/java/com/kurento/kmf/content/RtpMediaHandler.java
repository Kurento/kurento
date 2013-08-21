package com.kurento.kmf.content;

public interface RtpMediaHandler {

	void onMediaRequest(RtpMediaRequest request) throws ContentException;

	void onMediaTerminated(String requestId);

	void onMediaError(String requestId, ContentException exception);
}
