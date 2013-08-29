package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;

public interface PlayRequest {

	public String getContentId();

	public Object getAttribute(String name);

	public Object setAttribute(String name, Object value);

	public Object removeAttribute(String name);

	public HttpServletRequest getHttpServletRequest();

	public MediaPipelineFactory getMediaPipelineFactory();

	public void play(String contentPath) throws ContentException;

	public void usePlayer(PlayerEndPoint player);

	public void play(MediaElement source) throws ContentException;

	public void reject(int statusCode, String message);
}
