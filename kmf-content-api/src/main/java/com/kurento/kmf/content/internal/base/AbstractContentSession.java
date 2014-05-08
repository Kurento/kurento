/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.content.internal.base;

import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_EXECUTE;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_POLL;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_START;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_TERMINATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.SecretGenerator;
import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.ContentHandler;
import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.ControlEvent;
import com.kurento.kmf.content.internal.ControlProtocolManager;
import com.kurento.kmf.content.jsonrpc.Constraints;
import com.kurento.kmf.content.jsonrpc.JsonRpcConstants;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcContentEvent;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcControlEvent;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.repository.Repository;

/**
 * 
 * Generic content request processor.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class AbstractContentSession implements ContentSession {

	private static final Logger log = LoggerFactory
			.getLogger(AbstractContentSession.class);

	/*
	 * This variable is used as an indication of a client having a sessionId.
	 * The variable is useful for the case in which a session is initiated
	 * through a "execute" request. When registered=false, the client does not
	 * have a sessionId, when true, the client has got one. ACTIVE state =>
	 * registered = true. However, for IDLE, HANDLING and STARTING registered
	 * may be false (in case the "start" request is creating the session) or may
	 * be true (in case a previous execute command created the session)
	 */
	private volatile boolean registered = false;

	/*
	 * This state refers to the media exchange (media session state) and it
	 * mainly depends on the status of execution of "start" requests.
	 */
	protected enum STATE {
		IDLE, // No session yet
		HANDLING, // onContentRequest on execution in handler
		STARTING, // start on execution in session
		ACTIVE, // start answer was sent to client
		TERMINATED // session is terminated, no more requests accepted
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

	@Autowired
	private Repository repository;

	private ContentHandler<? extends ContentSession> handler;

	// List of Media Object to be cleaned-up (released) when this object
	// terminates.
	private Set<MediaObject> cleanupSet;

	private volatile STATE state;

	private ContentSessionManager manager;

	protected String sessionId;

	private String contentId;

	protected AsyncContext initialAsyncCtx;

	protected JsonRpcRequest initialJsonRequest;

	private ConcurrentHashMap<String, Object> attributes;

	protected BlockingQueue<Object> eventQueue;

	private Thread currentPollingThread;

	// /////////////////////////////////////////////////////
	// Abstract methods to be implemented by derived classes
	// /////////////////////////////////////////////////////

	protected abstract Logger getLogger();

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
		eventQueue = new LinkedBlockingQueue<Object>();
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
	public synchronized void releaseOnTerminate(MediaObject mediaObject) {
		if (state == STATE.TERMINATED) {
			mediaObject.release(new Continuation<Void>() {

				@Override
				public void onSuccess(Void result) {

				}

				@Override
				public void onError(Throwable cause) {

				}
			});
			throw new KurentoMediaFrameworkException(
					"Session is in TERMINATED state. Cannot invoke releaseOnTerminate any longer",
					1);// TODO
		}
		if (cleanupSet == null)
			cleanupSet = new HashSet<MediaObject>();
		cleanupSet.add(mediaObject);
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
		try {
			return initialJsonRequest.getParams().getConstraints()
					.getVideoContraints();
		} catch (NullPointerException e) {
			return null;
		}
	}

	public Constraints getAudioConstraints() {
		try {
			return initialJsonRequest.getParams().getConstraints()
					.getAudioContraints();
		} catch (NullPointerException e) {
			return null;
		}
	}

	// TODO: This method makes no sense when one wish to use HttpServletRequest
	// on an session initiated by a "execute" request. We should make possible
	// to access the specific HttpServletRequest associated to a method
	// execution
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
		eventQueue.add(contentEvent);
	}

	protected void publishControlEvent(ControlEvent controlEvent) {
		eventQueue.add(controlEvent);
	}

	// /////////////////////////////////////////////////////
	// Methods used by framework to reach the handler
	// /////////////////////////////////////////////////////

	private void callOnSessionTerminatedOnHandler(int code, String description) {
		try {
			interalRawCallToOnSessionTerminated(code, description);
		} catch (Throwable t) {
			getLogger().error(
					"Error invoking onContentCompleted on handler. Cause "
							+ t.getMessage(), t);
			callOnUncaughtExceptionThrown(t);
		}
	}

	protected abstract void interalRawCallToOnSessionTerminated(int code,
			String description) throws Exception;

	protected void callOnContentStartedOnHanlder() {
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

	private void callOnContentErrorOnHandler(int code, String description) {
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

	protected void callOnContentRequestOnHandler() {
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
		try {
			internalRawCallToOnUncaughtExceptionThrown(t);
		} catch (Throwable tw) {
			log.error(
					"Uncaught exception thrown while processing a content request",
					t);
			log.error(
					"Exception thrown while processing the uncaught exception",
					tw);
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
			initialAsyncCtx = asyncCtx;
			initialJsonRequest = message;
			// Check validity of constraints before making them accessible to
			// the handler
			Assert.notNull(
					initialJsonRequest.getParams().getConstraints()
							.getVideoContraints(),
					"Malfored request message specifying inexistent or invalid video contraints",
					10009);
			Assert.notNull(
					initialJsonRequest.getParams().getConstraints()
							.getVideoContraints(),
					"Malfored request message specifying inexistent or invalid audio contraints",
					10010);
			processStartJsonRpcRequest(asyncCtx, message);
		} else if (message.getMethod().equals(METHOD_POLL)) {
			Assert.isTrue(state != STATE.TERMINATED, "Cannot poll on state "
					+ state, 10011);
			Assert.isTrue(registered == true,
					"Cannot poll on unregistered state " + state, 10011); // TODO
																			// code

			ConsumeEventsResultType events = consumeEvents();
			protocolManager.sendJsonAnswer(asyncCtx, JsonRpcResponse
					.newPollResponse(events.contentEvents,
							events.controlEvents, message.getId()));
		} else if (message.getMethod().equals(METHOD_EXECUTE)) {
			Assert.isTrue(state != STATE.TERMINATED,
					"Cannot execute command on state " + state, 10011);
			internalProcessCommandExecution(asyncCtx, message);
		} else if (message.getMethod().equals(METHOD_TERMINATE)) {
			internalTerminateWithoutError(asyncCtx, message.getParams()
					.getReason().getCode(), message.getParams().getReason()
					.getMessage(), message);
		} else {
			throw new KurentoMediaFrameworkException(
					"Unrecognized message with method " + message.getMethod(),
					10012);
		}
	}

	private void internalProcessCommandExecution(AsyncContext asyncCtx,
			JsonRpcRequest message) {
		Assert.notNull(message.getParams(), "", 1); // TODO
		Assert.notNull(message.getParams().getCommand(), "", 1); // TODO
		ContentCommandResult result = null;
		try {
			result = interalRawCallToOnContentCommand(new ContentCommand(
					message.getParams().getCommand().getType(), message
							.getParams().getCommand().getData()));

		} catch (Throwable t) {
			getLogger().error(
					"Error invoking onContentCommand on handler. Cause "
							+ t.getMessage(), t);

			int errorCode = 1; // TODO: define error code
			if (t instanceof KurentoMediaFrameworkException) {
				errorCode = ((KurentoMediaFrameworkException) t).getCode();
			}

			if (!registered) {
				// An error with a command acting a session creation (e.g.
				// register) makes the session to terminate. This avoids
				// security problems where a client may force and exception, in
				// which case it should not be given access to the session.
				internalTerminateWithError(asyncCtx, errorCode, t.getMessage(),
						message);
			} else {
				// An error executing a command in other scenarion is not
				// considered faltal and the session may continue
				protocolManager.sendJsonError(asyncCtx, JsonRpcResponse
						.newError(errorCode, t.getMessage(), message.getId()));

			}

			callOnUncaughtExceptionThrown(t);
			return;
		}

		try {
			protocolManager.sendJsonAnswer(
					asyncCtx,
					JsonRpcResponse.newExecuteResponse(sessionId,
							result.getResult(), message.getId()));

			registered = true;

		} catch (Throwable t) {
			getLogger()
					.error("Error invoking sendJsonAnswer. Cause "
							+ t.getMessage(), t);

			int errorCode = 1; // TODO: define error code
			if (t instanceof KurentoMediaFrameworkException) {
				errorCode = ((KurentoMediaFrameworkException) t).getCode();
			}
			internalTerminateWithError(asyncCtx, errorCode, t.getMessage(),
					message);
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

	private static class ConsumeEventsResultType {
		private JsonRpcContentEvent[] contentEvents;
		private JsonRpcControlEvent[] controlEvents;
	}

	public ConsumeEventsResultType consumeEvents() {

		if (currentPollingThread != null) {
			currentPollingThread.interrupt();
		}

		currentPollingThread = Thread.currentThread();

		List<JsonRpcContentEvent> contentEvents = null;
		List<JsonRpcControlEvent> controlEvents = null;
		while (true) {
			Object event;
			try {
				event = eventQueue.poll(contentApiConfiguration
						.getWebRtcEventQueuePollTimeout(),
						TimeUnit.MILLISECONDS);

			} catch (InterruptedException e) {
				break;
			}

			if (event != null && event instanceof ContentEvent) {
				if (contentEvents == null)
					contentEvents = new ArrayList<JsonRpcContentEvent>();
				contentEvents.add(JsonRpcContentEvent.newEvent(
						((ContentEvent) event).getType(),
						((ContentEvent) event).getData()));
			}

			if (event != null && event instanceof ControlEvent) {
				if (controlEvents == null)
					controlEvents = new ArrayList<JsonRpcControlEvent>();
				controlEvents.add(JsonRpcControlEvent.newEvent(
						((ControlEvent) event).getType(),
						((ControlEvent) event).getCode(),
						((ControlEvent) event).getMessage()));
			}

			if (eventQueue.isEmpty()) {
				break;
			}
		}

		currentPollingThread = null;

		ConsumeEventsResultType result = new ConsumeEventsResultType();
		if (contentEvents != null) {
			result.contentEvents = contentEvents
					.toArray(new JsonRpcContentEvent[1]);
		}

		if (controlEvents != null) {
			result.controlEvents = controlEvents
					.toArray(new JsonRpcControlEvent[1]);
		}

		return result;

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
		internalTerminateWithoutError(null, code, description, null);
	}

	/**
	 * This method must be invoked by the framework whenever a session needs to
	 * be terminated with an error condition. This may occur due to different
	 * reasons, such as:
	 * 
	 * - Exception thrown by the communication layer while processing a message.<br>
	 * - Asynchronous error comming from the media server. <br>
	 * - Exception thrown when processing a "start" call on the session <br>
	 * 
	 * @param asyncCtx
	 *            represents the client request where the error has emerged. May
	 *            be null for async errors coming from the media server or when
	 *            the error occurs in the initialAsyncCtx (ex. while executing a
	 *            start call on the session). If this parameter is non-null, the
	 *            request parameter must also be non-null (except for
	 *            useControlProtocol=false).
	 * @param code
	 * @param description
	 * @param request
	 *            represents the client request that is causing this
	 *            termination. May be null if termination comes from async media
	 *            event or from invocation to terminate method on session.
	 */
	public void internalTerminateWithError(AsyncContext asyncCtx, int code,
			String description, JsonRpcRequest request) {
		// This method cannot throw exceptions

		getLogger().info("internalTerminateWithError called");

		STATE localState = null;
		synchronized (this) {

			localState = state;
			state = STATE.TERMINATED;

			try {

				if (asyncCtx != null) {
					// If we have an asyncCtx, we nust need to answer on it
					sendErrorAnswerOnSpecificContext(asyncCtx, code,
							description, request);
				} else {
					// Here, we have an error without asyncCtx specified. What
					// we
					// need to do depends on state
					if (localState == STATE.IDLE) {
						// This case represents and error without having a start
						// answer pending
						if (registered) {
							pushErrorEvent(code, description);
						} // Else, we can do nothing, just terminate silently
					} else if (localState == STATE.HANDLING
							|| localState == STATE.STARTING) {
						// Here we have a start answer pending, initialAsyncCtx
						// MUST
						// be non-null
						sendErrorAnswerOnInitialContext(code, description);
					} else if (localState == STATE.ACTIVE) {
						// This case represents an async error coming from the
						// media
						// server. Variable registered MUST be true here.
						pushErrorEvent(code, description);
					}
				}

			} catch (Throwable t) {
				getLogger().error(t.getMessage(), t);
			} finally {
				destroy();
			}
		}

		if (localState != null && localState != STATE.TERMINATED) {
			// We only want to call the handler once when the session terminates
			callOnContentErrorOnHandler(code, description);
		}
	}

	protected void sendErrorAnswerOnInitialContext(int code, String description) {
		protocolManager.sendJsonAnswer(
				initialAsyncCtx,
				JsonRpcResponse.newError(code, description,
						initialJsonRequest.getId()));
	}

	private void sendErrorAnswerOnSpecificContext(AsyncContext asyncCtx,
			int code, String description, JsonRpcRequest request) {
		protocolManager.sendJsonAnswer(asyncCtx,
				JsonRpcResponse.newError(code, description, request.getId()));
	}

	private void pushErrorEvent(int code, String description) {
		publishControlEvent(new ControlEvent(
				JsonRpcConstants.EVENT_SESSION_ERROR, code, description));
	}

	/**
	 * This method needs to be invoked when a session terminate without error.
	 * This may happen due to several reasons:
	 * 
	 * - Client sends a terminate message <br>
	 * - Handler invokes terminate on the session <br>
	 * 
	 * @param asyncCtx
	 *            represents the context of the client request causing this
	 *            termination. If this parameter is non-null, request must be
	 *            non-null (except for useControlProtocol=false).
	 * @param code
	 * @param description
	 * @param request
	 *            represents the client request that is causing this
	 *            termination. May be null if termination comes from async media
	 *            event or from invocation to terminate method on session.
	 */
	public void internalTerminateWithoutError(AsyncContext asyncCtx, int code,
			String description, JsonRpcRequest request) {
		// This method cannot throw exceptions

		getLogger().info("internalTerminateWithoutError called");

		STATE localState = null;

		synchronized (this) {

			localState = state;
			state = STATE.TERMINATED;

			try {
				if (asyncCtx != null) {
					// This can only happen upon execution of a client explicit
					// terminate command
					sendAckToExplicitClientTermination(asyncCtx, code,
							description, request);
				} else {
					if (localState == STATE.IDLE) {
						// This case represents an explicit terminate invocation
						// on
						// the session before start occurs.
						if (registered) {
							pushTerminateEvent(code, description);
						}
					} else if (localState == STATE.HANDLING
							|| localState == STATE.STARTING) {
						// This case represents an explicit terminate invocation
						// on
						// session when processing a start request
						sendRejectOnInitialContext(code, description);
					} else if (localState == STATE.ACTIVE) {
						pushTerminateEvent(code, description);
					}
				}

			} catch (Throwable t) {
				getLogger().error(t.getMessage(), t);
			} finally {
				destroy();
			}
		}

		if (localState != null && localState != STATE.TERMINATED) {
			// We only want to call the handler once when the session terminates
			callOnSessionTerminatedOnHandler(code, description);
		}
	}

	private void sendAckToExplicitClientTermination(AsyncContext asyncCtx,
			int code, String description, JsonRpcRequest request) {
		protocolManager.sendJsonAnswer(
				asyncCtx,
				JsonRpcResponse.newTerminateResponse(code, description,
						request.getId()));
	}

	private void pushTerminateEvent(int code, String description) {
		publishControlEvent(new ControlEvent(
				JsonRpcConstants.EVENT_SESSION_TERMINATED, code, description));
	}

	protected void sendRejectOnInitialContext(int code, String description) {
		protocolManager.sendJsonAnswer(initialAsyncCtx, JsonRpcResponse
				.newStartRejectedResponse(code, description,
						initialJsonRequest.getId()));
	}

	protected synchronized void destroy() {
		registered = false;

		if (initialAsyncCtx != null) {
			try {
				initialAsyncCtx.complete();
			} catch (IllegalStateException e) {
				log.warn("Exception try to complete initialAsyncCtx: {}", e
						.getClass().getName());
				// FIXME: We ignore this exception because is thrown when
				// asyncContext in yet in COMPLETING STATE.
			}
			initialAsyncCtx = null;
		}
		if (manager != null) {
			manager.remove(this.sessionId);
		}

		if (eventQueue.isEmpty() && currentPollingThread != null) {
			currentPollingThread.interrupt();
		}

		try {
			releaseOwnMediaServerResources();
		} catch (Throwable e) {
			getLogger().error(e.getMessage(), e);
		}
	}

	private synchronized void releaseOwnMediaServerResources() {
		if (cleanupSet == null) {
			return;
		}
		for (MediaObject mediaObject : cleanupSet) {
			getLogger()
					.info("Requesting release of MediaObject " + mediaObject);

			try {
				mediaObject.release(new Continuation<Void>() {
					@Override
					public void onSuccess(Void result) {
					}

					@Override
					public void onError(Throwable cause) {
						getLogger().warn(
								"Error releasing MediaObject: "
										+ cause.getMessage());
					}
				});
			} catch (Throwable t) {
				throw new KurentoMediaFrameworkException(
						"Error releasing media object " + mediaObject
								+ ". Cause: " + t.getMessage(), t, 20022);
			}
		}
		cleanupSet.clear();
		cleanupSet = null;
	}

	public Repository getRepository() {
		return repository;
	}

	protected synchronized void goToState(STATE target, String errorMessage,
			int errorCode) {
		STATE initial = state;
		try {
			if (target == STATE.HANDLING) {
				Assert.isTrue(initial == STATE.IDLE, errorMessage, errorCode);
				state = STATE.HANDLING;
			} else if (target == STATE.STARTING) {
				Assert.isTrue(initial == STATE.HANDLING, errorMessage,
						errorCode);
				state = STATE.STARTING;
			} else if (target == STATE.ACTIVE) {
				Assert.isTrue(state == STATE.STARTING, errorMessage, errorCode);
				state = STATE.ACTIVE;
				registered = true; // at this stage, user is registered with
									// certainty
			} else if (target == STATE.TERMINATED) {
				state = STATE.TERMINATED;
			}
		} finally {
			/*
			 * Whenever a thread wants to change the session state, if that
			 * state is already terminated, that means that another thread
			 * terminated the session. However, the thread calling this method
			 * may have created media elements or other resources. For this
			 * reason, we need to call destroy to guarantee that those resources
			 * are collected.
			 */
			if (initial == STATE.TERMINATED) {
				internalTerminateWithError(
						null,
						1,
						"Spureous state change attemp while being already termianted",
						null); // TODO: error code
			}
		}
	}

	protected synchronized STATE getState() {
		return state;
	}
}
