package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kms.media.MediaElement;

public interface PlayRequest {

	public String getContentId();

	public HttpServletRequest getHttpServletRequest();

	public void play(String contentPath) throws ContentException;

	public void play(MediaElement element) throws ContentException;

	public void reject(int statusCode, String message);
}
