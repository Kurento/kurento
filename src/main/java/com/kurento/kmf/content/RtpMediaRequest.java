package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipelineFactory;

public interface RtpMediaRequest {
	String getSessionId();

	String getContentId();

	public Constraints getVideoConstraints();

	public Constraints getAudioConstraints();

	public MediaPipelineFactory getMediaPipelineFactory();

	HttpServletRequest getHttpServletRequest();

	void startMedia(MediaElement sinkElement, MediaElement sourceElement)
			throws ContentException;

	void reject(int statusCode, String message);
}
