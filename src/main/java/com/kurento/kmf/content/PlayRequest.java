package com.kurento.kmf.content;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kms.media.MediaElement;

public abstract class PlayRequest {

	public abstract String getContentId();

	public abstract HttpServletRequest getHttpServletRequest();

	public abstract void play(String contentPath) throws IOException;

	public abstract void play(MediaElement element);

	public abstract void reject();

}
