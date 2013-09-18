package com.kurento.kmf.content.internal.base;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.common.excption.internal.ExceptionUtils;
import com.kurento.kmf.common.excption.internal.ServletUtils;
import com.kurento.kmf.content.ContentHandler;
import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.StreamingProxy;
import com.kurento.kmf.content.internal.StreamingProxyListener;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.HttpEndPointEvent;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaEventListener;

/**
 * 
 * Abstract definition for HTTP based content request.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class AbstractHttpBasedContentSession extends
		AbstractContentSession {

	@Autowired
	private StreamingProxy proxy;

	protected boolean useControlProtocol;

	protected boolean redirect;

	protected volatile Future<?> tunnellingProxyFuture;

	public AbstractHttpBasedContentSession(
			ContentHandler<? extends ContentSession> handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(handler, manager, asyncContext, contentId);
		this.useControlProtocol = useControlProtocol;
		this.redirect = redirect;
		if (!useControlProtocol) {
			state = STATE.HANDLING;
		}
	}

	/**
	 * Build media element (such as player, recorder, and so on) in the media
	 * server.
	 * 
	 * @param contentPath
	 *            Content path in which build the media element
	 * @return Created media element
	 */
	protected abstract MediaElement buildRepositoryBasedMediaElement(
			String contentPath);

	/**
	 * 
	 * @param mediaElements
	 *            must be non-null and non-empty
	 * @return
	 */
	protected abstract HttpEndPoint buildAndConnectHttpEndPointMediaElement(
			MediaElement... mediaElements);

	/*
	 * This is an utility method designed for minimizing code replication. For
	 * it to work, one and only one of the two parameters must be null;
	 */
	protected void activateMedia(String contentPath,
			MediaElement... mediaElements) {
		synchronized (this) {
			Assert.isTrue(state == STATE.HANDLING,
					"Cannot start media exchange in state " + state
							+ ". This error means ...", 10001); // TODO further
			// explanation
			state = STATE.STARTING;
		}

		boolean mediaElementProvided = mediaElements != null
				& mediaElements.length > 0;

		Assert.isTrue(
				mediaElementProvided || contentPath == null,
				"Internal error. Cannot process request containing two null parameters",
				10002);
		Assert.isTrue(
				mediaElementProvided || contentPath != null,
				"Internal error. Cannot process request containing two non null parameters",
				10003);

		getLogger().info(
				"Activating media for " + this.getClass().getSimpleName()
						+ " with contentPath " + contentPath);

		if (contentPath != null) {
			mediaElements = new MediaElement[1];
			mediaElements[0] = buildRepositoryBasedMediaElement(contentPath);
		}

		HttpEndPoint httpEndPoint = buildAndConnectHttpEndPointMediaElement(mediaElements);

		// We need to assert that session was not rejected while we were
		// creating media infrastructure
		boolean terminate = false;
		synchronized (this) {
			if (state == STATE.TERMINATED) {
				terminate = true;
			} else if (state == STATE.STARTING) {
				state = STATE.ACTIVE;
			}
		}

		// If session was rejected, just terminate
		if (terminate) {
			getLogger()
					.info("Exiting due to terminate ... this should only happen on client's explicit termination");
			return;
		}

		// If session was not rejected (state=ACTIVE) we send an answer and
		// the initialAsyncCtx becomes useless
		String answerUrl = null;
		try {
			answerUrl = httpEndPoint.getUrl();
			getLogger().info("HttpEndPoint.getUrl = " + answerUrl);
		} catch (IOException ioe) {
			throw new KurentoMediaFrameworkException(
					"Error recovering URL from HttpEndPoint. Cause: "
							+ ioe.getMessage(), ioe, 20006);
		}
		Assert.notNull(answerUrl, "Received null url from HttpEndPoint", 20012);
		Assert.isTrue(answerUrl.length() > 0,
				"Received invalid empty url from media server", 20012);

		getLogger().info("HttpEndPoint URL is " + answerUrl);

		// Add listeners for generating events on handler
		httpEndPoint.addListener(new MediaEventListener<HttpEndPointEvent>() {
			@Override
			public void onEvent(HttpEndPointEvent event) {
				// TODO: here we should send onContentCompleted and
				// onContentStarted
			}
		});

		// TODO add listeners for generating onContentError to handler
		// httpEndPoint.getMediaPipeline().addListener

		if (useControlProtocol) {
			answerActivateMediaRequest4JsonControlProtocolConfiguration(answerUrl);
		} else {
			answerActivateMediaRequest4SimpleHttpConfiguration(answerUrl);
		}

	}

	/**
	 * Provide an HTTP response, depending of which redirect strategy is used:
	 * it could a redirect (HTTP 307, Temporary Redirect), or a tunneled
	 * response using the Streaming Proxy.
	 * 
	 * @param url
	 *            Content URL
	 * @throws ContentException
	 *             Exception in the media server
	 */
	private void answerActivateMediaRequest4SimpleHttpConfiguration(String url) {
		try {
			HttpServletResponse response = (HttpServletResponse) initialAsyncCtx
					.getResponse();
			HttpServletRequest request = (HttpServletRequest) initialAsyncCtx
					.getRequest();
			if (redirect) {
				getLogger().info("Sending redirect to " + url);
				response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
				response.setHeader("Location", url);
			} else {
				getLogger().info("Activating tunneling proxy to " + url);
				tunnellingProxyFuture = proxy.tunnelTransaction(request,
						response, url, new StreamingProxyListener() {

							@Override
							public void onProxySuccess() {
								tunnellingProxyFuture = null;
								// Parameters no matter, no answer will be sent
								// given that we are already in ACTIVE state
								terminate(0, "");
							}

							@Override
							public void onProxyError(String message,
									int errorCode) {
								tunnellingProxyFuture = null;
								// Parameters no matter, no answer will be sent
								// given that we are already in ACTIVE state
								terminate(errorCode, message);
							}
						});
			}
		} catch (Throwable t) {
			throw new KurentoMediaFrameworkException(t.getMessage(), t, 20013);
		} finally {
			if (redirect) {
				initialAsyncCtx.complete();
			}
			initialAsyncCtx = null;
		}
	}

	/**
	 * Provide an HTTP response, when a JSON signaling protocol strategy is
	 * used.
	 * 
	 * @param url
	 *            Content URL
	 * @throws ContentException
	 *             Exception in the media server
	 */
	private void answerActivateMediaRequest4JsonControlProtocolConfiguration(
			String url) {
		protocolManager.sendJsonAnswer(initialAsyncCtx,
				JsonRpcResponse.newStartUrlResponse(url, sessionId,
						initialJsonRequest.getId()));
		initialAsyncCtx = null;
		initialJsonRequest = null;
	}

	/**
	 * Control protocol accessor (getter).
	 * 
	 * @return Control protocol strategy
	 */
	@Override
	public boolean useControlProtocol() {
		return useControlProtocol;
	}

	/**
	 * Send error code when using JSON signaling protocol.
	 */
	@Override
	protected void sendOnTerminateErrorMessageInInitialContext(int code,
			String description) {
		if (useControlProtocol) {
			protocolManager.sendJsonError(initialAsyncCtx, JsonRpcResponse
					.newError(ExceptionUtils.getJsonErrorCode(code),
							description, initialJsonRequest.getId()));
		} else {
			try {
				ServletUtils.sendHttpError(
						(HttpServletRequest) initialAsyncCtx.getRequest(),
						(HttpServletResponse) initialAsyncCtx.getResponse(),
						ExceptionUtils.getHttpErrorCode(code), description);
			} catch (ServletException e) {
				getLogger().error(e.getMessage(), e); // TODO code
			}
		}
	}

	/**
	 * Release Streaming proxy.
	 */
	@Override
	protected void destroy() {
		super.destroy();

		Future<?> localTunnelingProxyFuture = tunnellingProxyFuture;
		if (localTunnelingProxyFuture != null) {
			localTunnelingProxyFuture.cancel(true);
			tunnellingProxyFuture = null;
		}
	}
}
