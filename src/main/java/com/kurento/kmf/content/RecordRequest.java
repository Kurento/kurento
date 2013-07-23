package com.kurento.kmf.content;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kms.api.MediaType;
import com.kurento.kms.media.HttpEndPoint;
import com.kurento.kms.media.MediaElement;
import com.kurento.kms.media.MediaManager;
import com.kurento.kms.media.MediaManagerFactory;
import com.kurento.kms.media.MediaSink;
import com.kurento.kms.media.MediaSrc;
import com.kurento.kms.media.RecorderEndPoint;

public class RecordRequest {

	private static final Logger log = LoggerFactory
			.getLogger(RecordRequest.class);

	private AsyncContext asyncContext;
	private String contentId;

	public RecordRequest(AsyncContext asyncContext, String contentId) {
		this.asyncContext = asyncContext;
		this.contentId = contentId;
	}

	public String getContentId() {
		return contentId;
	}

	public HttpServletRequest getHttpServletRequest() {
		return (HttpServletRequest) asyncContext.getRequest();
	}

	public void record(String contentPath) throws ContentException {

		if (!((HttpServletRequest) asyncContext.getRequest()).isAsyncStarted()) {
			return;
		}

		HttpEndPoint httpEndPoint = null;
		RecorderEndPoint recorderEndPoint = null;

		try {
			MediaManager mediaManager = MediaManagerFactory.getInstance(null,
					0, null).createMediaManager();
			httpEndPoint = mediaManager.getHttpEndPoint();
			recorderEndPoint = mediaManager.getUriEndPoint(
					RecorderEndPoint.class, contentPath);
			MediaSrc videoMediaSrc = recorderEndPoint
					.getMediaSrc(MediaType.VIDEO).iterator().next();
			MediaSink videoMediaSink = httpEndPoint
					.getMediaSink(MediaType.VIDEO).iterator().next();
			MediaSrc audioMediaSrc = recorderEndPoint
					.getMediaSrc(MediaType.AUDIO).iterator().next();
			MediaSink audioMediaSink = httpEndPoint
					.getMediaSink(MediaType.AUDIO).iterator().next();
			videoMediaSrc.connect(videoMediaSink);
			audioMediaSrc.connect(audioMediaSink);

			HttpServletResponse response = (HttpServletResponse) asyncContext
					.getResponse();
			response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
			response.setHeader("Location", httpEndPoint.getUri());
			// If this call is made asynchronous complete should be in the
			// continuation
			asyncContext.complete();
		} catch (Throwable t) {
			// TODO when final KMS version is ready, perhaps it will be
			// necessary to release httpEndPoint and recorderEndPoint resources.
			throw new ContentException(t.getMessage(), t);
		}
	}

	public void record(MediaElement element) throws ContentException {
		if (!((HttpServletRequest) asyncContext.getRequest()).isAsyncStarted()) {
			return;
		}

		HttpEndPoint httpEndPoint = null;

		try {
			// FIXME: mediaManager must be retrieved from received MediaElement
			MediaManager mediaManager = MediaManagerFactory.getInstance(null,
					0, null).createMediaManager();
			httpEndPoint = mediaManager.getHttpEndPoint();
			MediaSrc videoMediaSrc = element.getMediaSrc(MediaType.VIDEO)
					.iterator().next();
			MediaSink videoMediaSink = httpEndPoint
					.getMediaSink(MediaType.VIDEO).iterator().next();
			MediaSrc audioMediaSrc = element.getMediaSrc(MediaType.AUDIO)
					.iterator().next();
			MediaSink audioMediaSink = httpEndPoint
					.getMediaSink(MediaType.AUDIO).iterator().next();
			videoMediaSrc.connect(videoMediaSink);
			audioMediaSrc.connect(audioMediaSink);

			HttpServletResponse response = (HttpServletResponse) asyncContext
					.getResponse();
			response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
			response.setHeader("Location", httpEndPoint.getUri());
			// If this call is made asynchronous complete should be in the
			// continuation
			asyncContext.complete();
		} catch (Throwable t) {
			// TODO: when final KMS version is ready, perhaps it will be
			// necessary to release httpEndPoint resources.
			throw new ContentException(t.getMessage(), t);
		}
	}

	public void reject(int statusCode, String message) {
		if (!((HttpServletRequest) asyncContext.getRequest()).isAsyncStarted()) {
			return;
		}

		try {
			((HttpServletResponse) asyncContext.getResponse()).sendError(
					statusCode, message);
		} catch (IOException e) {
			log.error("Exception rejecting RecordRequest", e);
		} finally {
			asyncContext.complete();
		}
	}

}
