package com.kurento.kmf.content.internal;

import static com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonConstants.ERROR_APPLICATION_TERMINATION;
import static com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonConstants.METHOD_POLL;
import static com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonConstants.METHOD_START;
import static com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonConstants.METHOD_TERMINATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.WebRtcMediaRequest;
import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonEvent;
import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonRequest;
import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonResponse;
import com.kurento.kmf.media.MediaElement;

public class WebRtcMediaRequestImpl implements WebRtcMediaRequest {

	private static final Logger log = LoggerFactory
			.getLogger(WebRtcMediaRequestImpl.class);

	private static enum STATE {
		IDLE, HANDLING, STARTING, ACTIVE, TERMINATED
	};

	@Autowired
	private WebRtcControlProtocolManager protocolManager;
	@Autowired
	private SecretGenerator secretGenerator;
	@Autowired
	private ContentApiConfiguration contentApiConfiguration;

	private STATE state = STATE.IDLE;
	private WebRtcMediaHandler handler;
	private WebRtcMediaRequestManager manager;
	private String sessionId;
	private String contentId;
	private HttpServletRequest httpServletRequest;
	private String sdp;
	private AsyncContext startingAsyncCtx; // Belongs to first request of the
											// media session
	private WebRtcJsonRequest startingRequest;
	private BlockingQueue<WebRtcJsonEvent> eventQueue;

	// TODO: concurrency control should be checked in this class
	public WebRtcMediaRequestImpl(WebRtcMediaHandler handler,
			WebRtcMediaRequestManager manager, String contentId,
			HttpServletRequest httpServletRequest) {
		this.handler = handler;
		this.manager = manager;
		this.eventQueue = new LinkedBlockingQueue<WebRtcJsonEvent>();
		this.httpServletRequest = httpServletRequest;
	}

	@PostConstruct
	public void onAfterBeanInitialized() {
		sessionId = secretGenerator.nextSecret();
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public String getContentId() {
		return contentId;
	}

	@Override
	public HttpServletRequest getHttpServletRequest() {
		return httpServletRequest;
	}

	@Override
	public void startMedia(MediaElement upStream, MediaElement downStream)
			throws ContentException {
		Assert.isTrue(state == STATE.HANDLING,
				"Cannot reject WebRtcMediaRequest in state " + state);
		state = STATE.STARTING;
		// TODO Create WebRtcEndPoint
		// TODO Store media elements for later clean-up
		// TODO Provide SDP to WebRtcEndPoint
		// TODO connect endpoint to provided MediaElements
		// TODO send SDP as answer to client
		// TODO blocking calls here should be interruptible
		// TODO invalidate startingAsyncCtx and startingRequest

		log.debug("SDP received " + startingRequest.getSdp());

		// This answer is temporary, for debugging purposes (returning the same
		// SDP received)
		String answer = startingRequest.getSdp();

		try {
			// Send SDP as answer to client
			protocolManager.sendJsonAnswer(startingAsyncCtx, WebRtcJsonResponse
					.newStartResponse(answer, sessionId,
							startingRequest.getId()));
			state = STATE.ACTIVE;
		} catch (IOException e) {
			throw new ContentException(e);
		}
	}

	@Override
	public void reject(int statusCode, String message) {
		Assert.isTrue(state == STATE.HANDLING,
				"Cannot reject WebRtcMediaRequest in state " + state);
		terminate(startingAsyncCtx, ERROR_APPLICATION_TERMINATION, message,
				startingRequest.getId());
	}

	public void processControlMessage(AsyncContext asyncCtx,
			WebRtcJsonRequest message) throws ContentException, IOException {
		Assert.notNull(message, "Cannot process null message");

		if (message.getMethod().equals(METHOD_START)) {
			Assert.isTrue(state == STATE.IDLE, "Illegal message with method "
					+ message.getMethod() + " on state " + state);
			state = STATE.HANDLING;
			sdp = message.getSdp();
			Assert.notNull(sdp, "SDP cannot be null on message with method "
					+ message.getMethod());
			this.startingAsyncCtx = asyncCtx;
			this.startingRequest = message;
			handler.onMediaRequest(this);
		} else if (message.getMethod().equals(METHOD_POLL)) {
			Assert.isTrue(state == STATE.ACTIVE, "Cannot poll on state "
					+ state);
			protocolManager.sendJsonAnswer(asyncCtx, WebRtcJsonResponse
					.newEventsResponse(message.getId(), consumeEvents()));
		} else if (message.getMethod().equals(METHOD_TERMINATE)) {
			if (state == STATE.TERMINATED) {
				return;
			} else {
				protocolManager.sendJsonAnswer(asyncCtx,
						WebRtcJsonResponse.newAckResponse(message.getId()));
				terminateSilently();
				handler.onMediaTerminated(message.getSessionId());
			}
		} else {
			ContentException contentException = new ContentException(
					"Unrecognized message with method " + message.getMethod());
			handler.onMediaError(message.getSessionId(), contentException);
			throw contentException;
		}
	}

	public void terminate(AsyncContext asyncCtx, int code, String description,
			int requestId) {
		// this method cannot throw exceptions
		try {
			if (state == STATE.TERMINATED) {
				return;
			}
			state = STATE.TERMINATED;

			protocolManager.sendJsonError(asyncCtx,
					WebRtcJsonResponse.newError(code, description, requestId));

		} finally {
			destroy();
		}
	}

	public void terminateSilently() {
		if (state == STATE.TERMINATED) {
			return;
		}
		state = STATE.TERMINATED;
		destroy();
	}

	public void produceEvents(WebRtcJsonEvent... events) {
		eventQueue.addAll(Arrays.asList(events));
	}

	public WebRtcJsonEvent[] consumeEvents() {
		List<WebRtcJsonEvent> events = null;
		while (true) {
			WebRtcJsonEvent event;
			try {
				event = eventQueue.poll(contentApiConfiguration
						.getWebRtcEventQueuePollTimeout(),
						TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				break;
			}
			if (event != null) {
				if (events == null)
					events = new ArrayList<WebRtcJsonEvent>();
				events.add(event);
			}
			if (eventQueue.isEmpty()) {
				break;
			}
		}
		if (events == null) {
			return null;
		} else {
			return events.toArray(new WebRtcJsonEvent[1]);
		}
	}

	private void destroy() {
		// This method cannot throw exceptions
		manager.remove(this.sessionId);
		// TODO free WebRtcMediaElement resources and all stuff related to media
		// server
	}
}
