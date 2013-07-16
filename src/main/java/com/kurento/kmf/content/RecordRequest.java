package com.kurento.kmf.content;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kms.media.MediaElement;

public abstract class RecordRequest {

	public abstract String getContentId();

	public abstract HttpServletRequest getHttpServletRequest();

	public abstract void record(String contentPath) throws IOException;

	public abstract void record(MediaElement element);

	public abstract void reject();

}
