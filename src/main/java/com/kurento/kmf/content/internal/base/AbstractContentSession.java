package com.kurento.kmf.content.internal.base;

import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_EXECUTE;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_POLL;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_START;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_TERMINATE;

import java.io.IOException;
import java.util.ArrayList;
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

import com.kurento.kmf.common.SecretGenerator;
import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.common.excption.internal.ExceptionUtils;
import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.ContentHandler;
import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.ControlProtocolManager;
import com.kurento.kmf.content.jsonrpc.Constraints;
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
public abstract class AbstractContentSession implements ContentSession {

	protected enum STATE {
		IDLE, HANDLING, STARTING, ACTIVE, TERMINATED
	};

	// /////////////////////////////////////////////////////
	// Attributes
	// /////////////////////////////////////////////////////
	@Autowired
	protected MediaPipelineFactory mediaPipelineFactory;

	@Autowired
	protected ControlProtocolManager protocolManager;

	@Autowired
	protected SecretGenerator secretGenerator;

	@Autowired
	protected ContentApiConfiguration contentApiConfiguration;

	private ContentHandler<? extends ContentSession> handler;

	// List of Media Object to be cleaned-up (released) when this object
	// terminates.
	private List<MediaObject> cleanupList;

	protected volatile STATE state;

	private ContentSessionManager manager;

	protected String sessionId;

	private String contentId;

	protected AsyncContext initialAsyncCtx;

	protected JsonRpcRequest initialJsonRequest;

	private ConcurrentHashMap<String, Object> attributes;

	protected BlockingQueue<JsonRpcEvent> eventQueue;

	// /////////////////////////////////////////////////////
	// Abstract methods to be implemented by derived classes
	// /////////////////////////////////////////////////////

	protected abstract Logger getLogger();

	protected abstract void sendOnTerminateErrorMessageInInitialContext(
			int code, String description);

	// /////////////////////////////////////////////////////
	// Constructor and utility methods
	// /////////////////////////////////////////////////////

	public AbstractContentSession(
			ContentHandler<? extends ContentSession> handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId) {
		state = STATE.IDLE;
		this.handler = handler;
		this.manager = manager;
		this.initialAsyncCtx = asyncContext;
		this.contentId = contentId;
		eventQueue = new LinkedBlockingQueue<JsonRpcEvent>();
	}

	@PostConstruct
	void onAfterBeanInitialized() {
		sessionId = secretGenerator.nextSecret();
	}

	// Default implementation returns true. Some derived classes will override
	// this
	public boolean useControlProtocol() {
		return true;
	}

	protected ContentHandler<? extends ContentSession> getHandler() {
		return handler;
	}

	// /////////////////////////////////////////////////////
	// Simple methods inherited from base ContentSession
	// /////////////////////////////////////////////////////

	@Override
	public Object getAttribute(String name) {
		if (attributes == null) {
			return null;
		} else {
			return attributes.get(name);
		}
	}

	@Override
	public Object setAttribute(String name, Object value) {
		if (attributes == null) {
			attributes = new ConcurrentHashMap<String, Object>();
		}
		return attributes.put(name, value);
	}

	@Override
	public Object removeAttribute(String name) {
		if (attributes == null) {
			return null;
		}
		return attributes.remove(name);
	}

	@Override
	public MediaPipelineFactory getMediaPipelineFactory() {
		return mediaPipelineFactory;
	}

	@Override
	public void releaseOnTerminate(MediaObject mediaObject) {
		if (cleanupList == null)
			cleanupList = new ArrayList<MediaObject>();
		cleanupList.add(mediaObject);
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public String getContentId() {
		return contentId;
	}

	public Constraints getVideoConstraints() {
		return initialJsonRequest.getVideoConstraints();
	}

	public Constraints getAudioConstraints() {
		return initialJsonRequest.getAudioConstraints();
	}

	@Override
	public HttpServletRequest getHttpServletRequest() {
		if (state == STATE.ACTIVE || state == STATE.TERMINATED) {
			throw new KurentoMediaFrameworkException(
					"Cannot access initial HttpServletRequest after media negotiation phase. "
							+ "This error means that you cannot access the original HttpServletRequest after a response to it has been sent.",
					10006);
		}
		return (HttpServletRequest) initialAsyncCtx.getRequest();
	}

	@Override
	public void publishEvent(ContentEvent contentEvent) {
		eventQueue.add(JsonRpcEvent.newEvent(contentEvent.getType(),
				contentEvent.getData()));
	}

	// /////////////////////////////////////////////////////
	// Methods used by framework to reach the handler
	// /////////////////////////////////////////////////////

	public void callOnContentCompletedOnHandler() {
		// TODO: when WebSockets is supported, terminate shall send error in
		// ACTIVE state. In that case, we should review if terminating here
		// makes sense (no error message should be sent)
		terminate(0, "OK");
		try {
			interalRawCallToOnContentCompleted();
		} catch (Throwable t) {
			getLogger().error(
					"Error invoking onContentCompleted on handler. Cause "
							+ t.getMessage(), t);
			callOnUncaughtExceptionThrown(t);
		}
	}

	protected abstract void interalRawCallToOnContentCompleted()
			throws Exception;

	public void callOnContentStartedOnHanlder() {
		try {
			interalRawCallToOnContentStarted();
		} catch (Throwable t) {
			getLogger().error(
					"Error invoking onContentCompleted on handler. Cause "
							+ t.getMessage(), t);
			callOnUncaughtExceptionThrown(t);
		}
	}

	protected abstract void interalRawCallToOnContentStarted() throws Exception;

	protected abstract ContentCommandResult interalRawCallToOnContentCommand(
			ContentCommand command) throws Exception;

	public void callOnContentErrorOnHandler(int code, String description) {
		terminate(code, description);
		try {
			interalRawCallToOnContentError(code, description);
		} catch (Throwable t) {
			getLogger().error(
					"Error invoking onContentError on handler. Cause "
							+ t.getMessage(), t);
			callOnUncaughtExceptionThrown(t);
		}
	}

	protected abstract void interalRawCallToOnContentError(int code,
			String description) throws Exception;

	public void callOnContentRequestOnHandler() {
		try {
			internalRawCallToOnContentRequest();
		} catch (Throwable t) {
			getLogger().error(
					"Error invoking onContentRequest on handler. Cause "
							+ t.getMessage(), t);
			callOnUncaughtExceptionThrown(t);
		}
	}

	protected abstract void internalRawCallToOnContentRequest()
			throws Exception;

	public void callOnUncaughtExceptionThrown(Throwable t) {
		int code = 9999;
		if (t instanceof KurentoMediaFrameworkException) {
			code = ((KurentoMediaFrameworkException) t).getCode();
		}
		terminate(code, t.getMessage());
		try {
			internalRawCallToOnUncaughtExceptionThrown(t);
		} catch (Throwable tw) {
			callOnUncaughtExceptionThrown(tw);
		}
	}

	protected abstract void internalRawCallToOnUncaughtExceptionThrown(
			Throwable t) throws Exception;

	// /////////////////////////////////////////////////////
	// Complex utility methods used by framework
	// /////////////////////////////////////////////////////

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
			JsonRpcRequest message) {
		Assert.notNull(message, "Cannot process null message", 10007);

		if (message.getMethod().equals(METHOD_START)) {
			synchronized (this) {
				Assert.isTrue(state == STATE.IDLE,
						"Illegal message with method " + message.getMethod()
								+ " on state " + state, 10008);
				state = STATE.HANDLING;
			}
			initialJsonRequest = message;
			// Check validity of constraints before making them accessible to
			// the handler
			Assert.notNull(
					initialJsonRequest.getVideoConstraints(),
					"Malfored request message specifying inexistent or invalid video contraints",
					10009);
			Assert.notNull(
					initialJsonRequest.getAudioConstraints(),
					"Malfored request message specifying inexistent or invalid audio contraints",
					10010);
			processStartJsonRpcRequest(asyncCtx, message);
		} else if (message.getMethod().equals(METHOD_POLL)) {
			Assert.isTrue(state == STATE.ACTIVE, "Cannot poll on state "
					+ state, 10011);

			protocolManager.sendJsonAnswer(asyncCtx, JsonRpcResponse
					.newEventsResponse(message.getId(), consumeEvents()));
		} else if (message.getMethod().equals(METHOD_EXECUTE)) {
			Assert.isTrue(state == STATE.ACTIVE,
					"Cannot execute command on state " + state, 10011);
			internalProcessCommandExecution(asyncCtx, message);
		} else if (message.getMethod().equals(METHOD_TERMINATE)) {
			terminate(false, asyncCtx,
					0, // No error
					"Application requested explicit termiantion",
					message.getId());
		} else {
			throw new KurentoMediaFrameworkException(
					"Unrecognized message with method " + message.getMethod(),
					10012);
		}
	}

	private void internalProcessCommandExecution(AsyncContext asyncCtx,
			JsonRpcRequest message) {
		try {
			ContentCommandResult result = interalRawCallToOnContentCommand(new ContentCommand(
					message.getCommandType(), message.getCommandData()));
			protocolManager.sendJsonAnswer(
					asyncCtx,
					JsonRpcResponse.newCommandResponse(message.getId(),
							result.getResult()));
		} catch (Throwable t) {
			int errorCode = 1; // TODO: define error code
			if (t instanceof KurentoMediaFrameworkException) {
				errorCode = ((KurentoMediaFrameworkException) t).getCode();
			}

			protocolManager.sendJsonError(
					asyncCtx,
					JsonRpcResponse.newError(errorCode, t.getMessage(),
							message.getId()));

			getLogger().error(
					"Error invoking onContentCommand on handler. Cause "
							+ t.getMessage(), t);
			callOnUncaughtExceptionThrown(t);
		}
	}

	// Default implementation that may be overriden by derived classes
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) {
		callOnContentRequestOnHandler();
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
	 *            termination code TODO: explain where are the codes
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
				// Send error to connection of terminating session
				protocolManager.sendJsonError(asyncCtx, JsonRpcResponse
						.newError(ExceptionUtils.getJsonErrorCode(code),
								description, requestId));
			}
		} catch (Throwable e) {
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
	@Override
	public void terminate(int code, String description) {
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
				// TODO: when we support real push of signaling, we should
				// notify the client that the session is terminating. This
				// should not overlap with the processing of a "terminate"
				// request explicitly received from client
			}
		} catch (Throwable t) {
			getLogger().error(t.getMessage(), t);
		} finally {
			destroy();
		}
	}

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

	private void releaseOwnMediaServerResources() {
		if (cleanupList == null) {
			return;
		}
		for (MediaObject mediaObject : cleanupList) {
			getLogger()
					.info("Requesting release of MediaObject " + mediaObject);

			try {
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
			} catch (Throwable t) {
				throw new KurentoMediaFrameworkException(
						"Error releasing media object " + mediaObject
								+ ". Cause: " + t.getMessage(), t, 20022);
			}
		}
	}

	protected void connect(MediaElement sourceElement,
			MediaElement[] sinkElements) {
		Assert.notNull(sourceElement, "Cannot connect null source element",
				10014);
		Assert.notNull(sinkElements, "Cannot connect to null sinkElements",
				10015);
		Assert.isTrue(sinkElements.length > 0,
				"Cannot connect empty sinkElements", 10016);

		for (MediaElement sinkElement : sinkElements) {
			Assert.notNull(sinkElement, "Cannot connect to null sinkElement",
					10017);
			getLogger().info(
					"Connecting video source of " + sourceElement
							+ " to video Sink of " + sinkElement);
			try {
				MediaSink videoSink = sinkElement
						.getMediaSinks(MediaType.VIDEO).iterator().next();
				sourceElement.getMediaSrcs(MediaType.VIDEO).iterator().next()
						.connect(videoSink);
				getLogger().info(
						"Connected " + sourceElement + " to  " + sinkElement);
				// TODO: activate audio when possible
				// getLogger().info("Connecting audio source of " +
				// sourceElement
				// + " to audio Sink of " + sinkElement);
				// MediaSink audioSink =
				// sinkElement.getMediaSinks(MediaType.AUDIO)
				// .iterator().next();
				// sourceElement.getMediaSrcs(MediaType.AUDIO).iterator().next()
				// .connect(audioSink);
			} catch (IOException ioe) {
				throw new KurentoMediaFrameworkException(
						"Cannot connect source media element " + sourceElement
								+ " to sink media element " + sinkElement,
						20021);
			}
			getLogger().info("Connect successful  ...");
		}
	}
}
