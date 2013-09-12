package com.kurento.kmf.content.internal.base;

import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_POLL;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_START;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_TERMINATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.ControlProtocolManager;
import com.kurento.kmf.content.internal.SecretGenerator;
import com.kurento.kmf.content.jsonrpc.Constraints;
import com.kurento.kmf.content.jsonrpc.JsonRpcConstants;
import com.kurento.kmf.content.jsonrpc.JsonRpcEvent;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kms.api.MediaType;

/**
 * 
 * Generic content request processor.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class AbstractContentRequest {

	/**
	 * 
	 * State machine for content request.
	 * 
	 */
	protected enum STATE {
		IDLE, HANDLING, STARTING, ACTIVE, TERMINATED
	};

	@Autowired
	protected MediaPipelineFactory mediaPipelineFactory;
	@Autowired
	protected ControlProtocolManager protocolManager;

	/**
	 * Autowired random word generator.
	 */
	@Autowired
	protected SecretGenerator secretGenerator;

	/**
	 * Autowired content API configuration.
	 */
	@Autowired
	protected ContentApiConfiguration contentApiConfiguration;

	/**
	 * List of Media Object to be cleaned.
	 */
	private List<MediaObject> cleanupList;

	/**
	 * Current state within the state machine.
	 */
	protected volatile STATE state;

	/**
	 * Content Request Manager.
	 */
	private ContentRequestManager manager;

	/**
	 * Session identifier.
	 */
	protected String sessionId;

	/**
	 * Content identifier.
	 */
	private String contentId;

	/**
	 * Asynchronous context.
	 */
	protected AsyncContext initialAsyncCtx;

	/**
	 * JSON request.
	 */
	protected JsonRpcRequest initialJsonRequest;

	/**
	 * JSON Attributes.
	 */
	private ConcurrentHashMap<String, Object> attributes;

	/**
	 * Event queue.
	 */
	protected BlockingQueue<JsonRpcEvent> eventQueue;

	// Abstract methods
	/**
	 * JSON rpc request processor.
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param message
	 *            JSON message (Java object)
	 * @throws ContentException
	 *             Exception processing request
	 */
	protected abstract void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException;

	/**
	 * Logger accessor (getter).
	 * 
	 * @return logger
	 */
	protected abstract Logger getLogger();

	/**
	 * Cancellation of the transmission.
	 */
	protected abstract void cancelMediaTransmission();

	/**
	 * Abstract declaration of terminate on error event.
	 * 
	 * @param code
	 *            Error code
	 * @param description
	 *            Error description
	 * @throws IOException
	 *             Input/Output exception
	 */
	protected abstract void sendOnTerminateErrorMessageInInitialContext(
			int code, String description) throws IOException;

	// Concrete methods and constructors
	/**
	 * Parameterized constructor.
	 * 
	 * @param manager
	 *            Content request manager
	 * @param asyncContext
	 *            Asynchronous context
	 * @param contentId
	 *            Content identifier
	 */
	public AbstractContentRequest(ContentRequestManager manager,
			AsyncContext asyncContext, String contentId) {
		state = STATE.IDLE;
		this.manager = manager;
		this.initialAsyncCtx = asyncContext;
		this.contentId = contentId;
		eventQueue = new LinkedBlockingQueue<JsonRpcEvent>();
	}

	public void addForCleanUp(MediaObject mediaObject) {
		if (cleanupList == null)
			cleanupList = new ArrayList<MediaObject>();
		cleanupList.add(mediaObject);
	}

	/**
	 * After construct method; it generates the random session identifier.
	 */
	@PostConstruct
	void onAfterBeanInitialized() {
		sessionId = secretGenerator.nextSecret();
	}

	/**
	 * Session identifier accessor (getter).
	 * 
	 * @return Session identifier
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Content identifier accessor (getter)
	 * 
	 * @return Content identifier
	 */
	public String getContentId() {
		return contentId;
	}

	/**
	 * Video constraints accessor (getter).
	 * 
	 * @return Video constraints
	 */
	public Constraints getVideoConstraints() {
		return initialJsonRequest.getVideoConstraints();
	}

	/**
	 * Audio constraints accessor (getter).
	 * 
	 * @return Audio constraints
	 */
	public Constraints getAudioConstraints() {
		return initialJsonRequest.getAudioConstraints();
	}

	/**
	 * HTTP Servlet request accessor (getter).
	 * 
	 * @return HTTP Servlet request
	 */
	public HttpServletRequest getHttpServletRequest() {
		if (state == STATE.ACTIVE || state == STATE.TERMINATED) {
			throw new IllegalStateException(
					"Cannot access initial HttpServletRequest after media negotiation phase. "
							+ "This error means that you cannot access the original HttpServletRequest after a response to it has been sent.");
		}
		return (HttpServletRequest) initialAsyncCtx.getRequest();
	}

	/**
	 * Manages the JSON message depending on the state.
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param message
	 *            JSON message (Java object)
	 * @throws ContentException
	 *             Exception in processing, typically when unrecognized message
	 * @throws IOException
	 *             Input/Output exception
	 */
	public void processControlMessage(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException, IOException {
		Assert.notNull(message, "Cannot process null message");

		if (message.getMethod().equals(METHOD_START)) {
			synchronized (this) {
				Assert.isTrue(state == STATE.IDLE,
						"Illegal message with method " + message.getMethod()
								+ " on state " + state);
				state = STATE.HANDLING;
			}
			initialJsonRequest = message;
			// Check validity of constraints before making them accessible to
			// the handler
			Assert.notNull(initialJsonRequest.getVideoConstraints(),
					"Malfored request message specifying inexistent or invalid video contraints");
			Assert.notNull(initialJsonRequest.getAudioConstraints(),
					"Malfored request message specifying inexistent or invalid audio contraints");
			processStartJsonRpcRequest(asyncCtx, message);
		} else if (message.getMethod().equals(METHOD_POLL)) {
			Assert.isTrue(state == STATE.ACTIVE, "Cannot poll on state "
					+ state);

			protocolManager.sendJsonAnswer(asyncCtx, JsonRpcResponse
					.newEventsResponse(message.getId(), consumeEvents()));
		} else if (message.getMethod().equals(METHOD_TERMINATE)) {
			terminate(false, asyncCtx,
					JsonRpcConstants.ERROR_APPLICATION_TERMINATION,
					"Application requested explicit termiantion",
					message.getId());
		} else {
			ContentException contentException = new ContentException(
					"Unrecognized message with method " + message.getMethod());
			throw contentException;
		}
	}

	/**
	 * Reject request.
	 * 
	 * @param statusCode
	 *            Error code
	 * @param message
	 *            Error message
	 */
	public void reject(int statusCode, String message) {
		terminate(statusCode, message);
	}

	/**
	 * Send events to the client.
	 * 
	 * @param events
	 *            list of events to be sent
	 */
	public void produceEvents(JsonRpcEvent... events) {
		eventQueue.addAll(Arrays.asList(events));
	}

	/**
	 * Get poll events.
	 * 
	 * @return list of received poll events
	 */
	public JsonRpcEvent[] consumeEvents() {
		List<JsonRpcEvent> events = null;
		while (true) {
			JsonRpcEvent event;
			try {
				event = eventQueue.poll(contentApiConfiguration
						.getWebRtcEventQueuePollTimeout(),
						TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				break;
			}
			if (event != null) {
				if (events == null)
					events = new ArrayList<JsonRpcEvent>();
				events.add(event);
			}
			if (eventQueue.isEmpty()) {
				break;
			}
		}
		if (events == null) {
			return null;
		} else {
			return events.toArray(new JsonRpcEvent[1]);
		}
	}

	/**
	 * Terminates this object in the context of an AsyncContext different from
	 * the initial AsyncContext sending the appropriate messages if necessary.
	 * 
	 * @param withError
	 *            boolean value indicating whether the termination is due to an
	 *            error or not
	 * @param asyncCtx
	 *            the AsynContext of the request that produced the termination
	 *            due to an explicit termination request or due to an error in
	 *            the processing of the request.
	 * @param code
	 *            termination code
	 * @param description
	 *            termination message
	 * @param requestId
	 *            request identifier
	 */
	public void terminate(boolean withError, AsyncContext asyncCtx, int code,
			String description, int requestId) {

		// If we are terminating from the initial AsyncContext, then no special
		// actions need to be taken
		if (asyncCtx == initialAsyncCtx) {
			terminate(code, description);
			return;
		}

		// If we are terminating from a different AsyncContext, then we need to
		// answer the request appropriately
		try {
			if (!withError) {
				// Send ACK to connection requesting termination
				protocolManager.sendJsonAnswer(asyncCtx,
						JsonRpcResponse.newAckResponse(requestId));
			} else {
				JsonRpcResponse.newError(code, description, requestId);
			}
		} catch (IOException e) {
			getLogger().debug(e.getMessage(), e);
		} finally {
			terminate(code, description);
		}
	}

	/**
	 * Terminates this object, completing initialAsyncCtx if necessary and
	 * sending an answer to the initial request if necessary.
	 * 
	 * @param code
	 *            termination code
	 * @param description
	 *            termination description
	 */
	protected void terminate(int code, String description) {
		// This method cannot throw exceptions

		STATE localState;
		synchronized (this) {
			if (state == STATE.TERMINATED)
				return;
			localState = state;
			state = STATE.TERMINATED;
		}

		try {
			if (localState == STATE.IDLE || localState == STATE.HANDLING
					|| localState == STATE.STARTING) {
				sendOnTerminateErrorMessageInInitialContext(code, description);
			} else if (localState == STATE.ACTIVE) {
				cancelMediaTransmission();
			}
		} catch (Throwable t) {
			getLogger().error(t.getMessage(), t);
		} finally {
			destroy();
		}
	}

	/**
	 * Asynchronous context completion.
	 */
	protected void destroy() {
		if (initialAsyncCtx != null) {
			initialAsyncCtx.complete();
			initialAsyncCtx = null;
		}
		if (manager != null) {
			manager.remove(this.sessionId);
		}

		try {
			releaseOwnMediaServerResources();
		} catch (Throwable e) {
			getLogger().error(e.getMessage(), e);
		}
	}

	/**
	 * Release media server resources.
	 * 
	 * @throws Throwable
	 *             Error/exception happen releasing media server resources
	 */
	protected void releaseOwnMediaServerResources() throws Throwable {
		if (cleanupList == null) {
			return;
		}
		for (MediaObject mediaObject : cleanupList) {
			getLogger()
					.info("Requesting release of MediaObject " + mediaObject);
			mediaObject.release(new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) {
				}

				@Override
				public void onError(Throwable cause) {
					getLogger().error(
							"Error releasing MediaObject: "
									+ cause.getMessage(), cause);
				}
			});
		}
	}

	/**
	 * Media Pipeline Factory accessor (getter).
	 * 
	 * @return Media Pipeline Factory
	 */
	public MediaPipelineFactory getMediaPipelineFactory() {
		// TODO: this returned class should be a wrapper of the real class so
		// that when the user creates a resource the request stores the resource
		// for later cleanup
		return mediaPipelineFactory;
	}

	/**
	 * Connect media element (source and sink).
	 * 
	 * @param sourceElement
	 *            Out-going media element
	 * @param sinkElement
	 *            In-going media element
	 * @throws IOException
	 *             Input/Output exception
	 */
	protected void connect(MediaElement sourceElement, MediaElement sinkElement)
			throws IOException {
		getLogger().info(
				"Connecting video source of " + sourceElement
						+ " to video Sink of " + sinkElement);
		MediaSink videoSink = sinkElement.getMediaSinks(MediaType.VIDEO)
				.iterator().next();
		sourceElement.getMediaSrcs(MediaType.VIDEO).iterator().next()
				.connect(videoSink);
		getLogger().info("Connected " + sourceElement + " to  " + sinkElement);
		// TODO: activate audio when possible
		// getLogger().info("Connecting audio source of " + sourceElement
		// + " to audio Sink of " + sinkElement);
		// MediaSink audioSink = sinkElement.getMediaSinks(MediaType.AUDIO)
		// .iterator().next();
		// sourceElement.getMediaSrcs(MediaType.AUDIO).iterator().next()
		// .connect(audioSink);
		getLogger().info("Connect successful  ...");
	}

	/**
	 * Attribute (by name) accessor (getter).
	 * 
	 * @param name
	 *            Attribute name
	 * @return Attribute value
	 */
	public Object getAttribute(String name) {
		if (attributes == null) {
			return null;
		} else {
			return attributes.get(name);
		}
	}

	/**
	 * Attribute mutator (setter), using a pair key-value.
	 * 
	 * @param name
	 *            Attribute name (key)
	 * @param value
	 *            Attribute value
	 * @return Attribute
	 */
	public Object setAttribute(String name, Object value) {
		if (attributes == null) {
			attributes = new ConcurrentHashMap<String, Object>();
		}
		return attributes.put(name, value);
	}

	/**
	 * Delete an attribute, identified by its name.
	 * 
	 * @param name
	 *            Attribute name
	 * @return Deleted attribute value
	 */
	public Object removeAttribute(String name) {
		if (attributes == null) {
			return null;
		}
		return attributes.remove(name);
	}
}
