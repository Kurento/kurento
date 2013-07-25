package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaElement;

public interface RecordRequest {

	public String getContentId();

	public HttpServletRequest getHttpServletRequest();

	public void record(String contentPath) throws ContentException;

	public void record(MediaElement element) throws ContentException;

	public void reject(int statusCode, String message);
}
