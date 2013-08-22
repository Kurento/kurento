package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaElement;

public interface RtpMediaRequest {
	String getSessionId();

	String getContentId();

	public Constraints getVideoConstraints();

	public Constraints getAudioConstraints();

	HttpServletRequest getHttpServletRequest();

	void startMedia(MediaElement upStream, MediaElement downStream)
			throws ContentException;

	void reject(int statusCode, String message);
}
