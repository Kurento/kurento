package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.content.jsonrpc.Constraints;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipelineFactory;

public interface WebRtcMediaRequest {
	String getSessionId();

	String getContentId();

	public Object getAttribute(String name);

	public Object setAttribute(String name, Object value);

	public Object removeAttribute(String name);

	public Constraints getVideoConstraints();

	public Constraints getAudioConstraints();

	HttpServletRequest getHttpServletRequest();

	public MediaPipelineFactory getMediaPipelineFactory();

	void startMedia(MediaElement sinkElement, MediaElement sourceElement)
			throws ContentException;

	void reject(int statusCode, String message);
}
