package com.kurento.kmf.content.internal;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaManager;
import com.kurento.kmf.media.MediaManagerFactory;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSrc;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kms.api.MediaType;

public class PlayRequestImpl implements PlayRequest {
	private static final Logger log = LoggerFactory
			.getLogger(PlayRequestImpl.class);

	private static enum STATE {
		IDLE, PLAYING, TERMINATED
	};

	private volatile STATE state;

	@Autowired
	private MediaManagerFactory mediaManagerFactory;

	@Autowired
	private StreamingProxy proxy;

	private AsyncContext asyncContext;
	private String contentId;
	private boolean redirect;
	private Future<?> tunnellingProxyFuture;

	PlayRequestImpl(AsyncContext asyncContext, String contentId,
			boolean redirect) {
		this.asyncContext = asyncContext;
		this.contentId = contentId;
		this.redirect = redirect;
		this.state = STATE.IDLE;
	}

	@Override
	public String getContentId() {
		return contentId;
	}

	@Override
	public HttpServletRequest getHttpServletRequest() {
		if (state != STATE.IDLE) {
			throw new IllegalStateException(
					"Cannot recover HttpServletRequest in state "
							+ state
							+ ". This error may be produced by several reasons including a previous invocation to play or "
							+ "reject or a timeout");
		}
		return (HttpServletRequest) asyncContext.getRequest();
	}

	@Override
	public void play(String contentPath) throws ContentException {

		synchronized (this) {
			if (state != STATE.IDLE) {
				throw new IllegalStateException(
						"Cannot invoke play after invoking play or reject in this object");
			}
			state = STATE.PLAYING;
		}

		HttpEndPoint httpEndPoint = null;
		PlayerEndPoint playerEndPoint = null;

		try {
			// TODO calls to blocking methods on media-api must be interruptible
			// (in a thread sense of the term). This applies to the whole API.
			MediaManager mediaManager = mediaManagerFactory.createMediaManager(
					null, 0, null);
			httpEndPoint = mediaManager.getHttpEndPoint();
			playerEndPoint = mediaManager.getUriEndPoint(PlayerEndPoint.class,
					contentPath);
			MediaSrc videoMediaSrc = playerEndPoint
					.getMediaSrc(MediaType.VIDEO).iterator().next();
			MediaSink videoMediaSink = httpEndPoint
					.getMediaSink(MediaType.VIDEO).iterator().next();
			MediaSrc audioMediaSrc = playerEndPoint
					.getMediaSrc(MediaType.AUDIO).iterator().next();
			MediaSink audioMediaSink = httpEndPoint
					.getMediaSink(MediaType.AUDIO).iterator().next();
			videoMediaSrc.connect(videoMediaSink);
			audioMediaSrc.connect(audioMediaSink);

			HttpServletResponse response = (HttpServletResponse) asyncContext
					.getResponse();
			HttpServletRequest request = (HttpServletRequest) asyncContext
					.getRequest();

			if (redirect) {
				response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
				response.setHeader("Location", httpEndPoint.getUrl());
			} else {
				tunnellingProxyFuture = proxy.tunnelTransaction(request,
						response, httpEndPoint.getUrl(),
						new StreamingProxyListener() {

							@Override
							public void onProxySuccess() {
								tunnellingProxyFuture = null;
								terminate();
							}

							@Override
							public void onProxyError(String message) {
								tunnellingProxyFuture = null;
								terminate();
							}
						});
			}

		} catch (Throwable t) {
			// TODO when final KMS version is ready, perhaps it will be
			// necessary to release httpEndPoint and playerEndPoint resources.
			throw new ContentException(t.getMessage(), t);
		} finally {
			// If this call is made asynchronous complete should be in the
			// continuation
			if (redirect) {
				asyncContext.complete();
			}
		}
	}

	@Override
	public void play(MediaElement element) throws ContentException {
		synchronized (this) {
			if (state != STATE.IDLE) {
				throw new IllegalStateException(
						"Cannot invoke play in state "
								+ state
								+ ". This is may be due to several reasons including a previous invocation to play or reject or "
								+ "a timemout.");
			}
			state = STATE.PLAYING;
		}

		HttpEndPoint httpEndPoint = null;

		try {
			// TODO: mediaManager must be retrieved from received MediaElement
			MediaManager mediaManager = mediaManagerFactory.createMediaManager(
					null, 0, null);
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
			HttpServletRequest request = (HttpServletRequest) asyncContext
					.getRequest();

			response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
			response.setHeader("Location", httpEndPoint.getUrl());

			if (redirect) {
				response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
				response.setHeader("Location", httpEndPoint.getUrl());
			} else {
				tunnellingProxyFuture = proxy.tunnelTransaction(request,
						response, httpEndPoint.getUrl(),
						new StreamingProxyListener() {

							@Override
							public void onProxySuccess() {
								tunnellingProxyFuture = null;
								terminate();
							}

							@Override
							public void onProxyError(String message) {
								tunnellingProxyFuture = null;
								terminate();
							}
						});
			}

		} catch (Throwable t) {
			// TODO when final KMS version is ready, perhaps it will be
			// necessary to release httpEndPoint resources.
			throw new ContentException(t.getMessage(), t);
		} finally {
			// If this call is made asynchronous complete should be in the
			// continuation
			if (redirect) {
				asyncContext.complete();
			}
		}
	}

	@Override
	public synchronized void reject(int statusCode, String message) {
		if (state == STATE.IDLE) {
			try {
				((HttpServletResponse) asyncContext.getResponse()).sendError(
						statusCode, message);
			} catch (IOException e) {
				log.error("Exception rejecting PlayRequest", e);
			} finally {
				terminate();
			}
		} else if (state == STATE.PLAYING) {
			terminate();
		} else if (state == STATE.TERMINATED) {
			return;
		}
	}

	public void terminate() {
		synchronized (this) {
			if (state == STATE.TERMINATED) {
				return;
			}
			state = STATE.TERMINATED;
		}

		try {
			synchronized (this) {
				if (tunnellingProxyFuture != null) {
					tunnellingProxyFuture.cancel(true);
					tunnellingProxyFuture = null;
				}
			}
			// TODO: Media resources created by this object should be stopped
			// and released
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		} finally {
			asyncContext.complete();
		}
	}
}
