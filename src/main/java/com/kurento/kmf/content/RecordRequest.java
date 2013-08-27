package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipelineFactory;

public interface RecordRequest {

	public String getContentId();

	public Object getAttribute(String name);

	public Object setAttribute(String name, Object value);

	public Object removeAttribute(String name);

	public HttpServletRequest getHttpServletRequest();

	public MediaPipelineFactory getMediaPipelineFactory();

	public void record(String contentPath) throws ContentException;

	public void record(MediaElement element) throws ContentException;

	public void reject(int statusCode, String message);
}
