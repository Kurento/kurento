package com.kurento.kmf.content.internal.base;

import static com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants.METHOD_POLL;
import static com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants.METHOD_START;
import static com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants.METHOD_TERMINATE;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.kurento.kmf.content.Constraints;
import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.ControlProtocolManager;
import com.kurento.kmf.content.internal.SecretGenerator;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcEvent;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaObject;

public abstract class AbstractContentRequest {
	protected enum STATE {
		IDLE, HANDLING, STARTING, ACTIVE, TERMINATED
	};

	@Autowired
	protected ControlProtocolManager protocolManager;
	@Autowired
	protected SecretGenerator secretGenerator;
	@Autowired
	protected ContentApiConfiguration contentApiConfiguration;

	private List<MediaObject> cleanupList;
	
	protected volatile STATE state;
	private ContentRequestManager manager;
	protected String sessionId;
	private String contentId;
	protected AsyncContext initialAsyncCtx;
	protected JsonRpcRequest initialJsonRequest;

	protected BlockingQueue<JsonRpcEvent> eventQueue;

	// Abstract methods
	protected abstract void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException;

	protected abstract Logger getLogger();

	protected abstract void cancelMediaTransmission();

	protected abstract void sendOnTerminateErrorMessageInInitialContext(
			int code, String description) throws IOException;

	// Concrete methods and constructors
	public AbstractContentRequest(ContentRequestManager manager,
			AsyncContext asyncContext, String contentId) {
		state = STATE.IDLE;
		this.manager = manager;
		this.initialAsyncCtx = asyncContext;
		this.contentId = contentId;
		eventQueue = new LinkedBlockingQueue<JsonRpcEvent>();
	}

	protected void addForCleanUp(MediaObject mediaObject){
		if(cleanupList == null)
			cleanupList = new ArrayList<MediaObject>();
		cleanupList.add(mediaObject);
	}
	
	@PostConstruct
	void onAfterBeanInitialized() {
		sessionId = secretGenerator.nextSecret();
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getContentId() {
		return contentId;
	}

	public Constraints getVideoConstraints() {
		return initialJsonRequest.getVideoConstraints();
	}

	public Constraints getAudioConstraints() {
		return initialJsonRequest.getAudioConstraints();
	}

	public HttpServletRequest getHttpServletRequest() {
		if (state == STATE.ACTIVE || state == STATE.TERMINATED) {
			throw new IllegalStateException(
					"Cannot access initial HttpServletRequest after media negotiation phase. "
							+ "This error means that you cannot access the original HttpServletRequest after a response to it has been sent.");
		}
		return (HttpServletRequest) initialAsyncCtx.getRequest();
	}

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

	public void reject(int statusCode, String message) {
		terminate(statusCode, message);
	}

	public void produceEvents(JsonRpcEvent... events) {
		eventQueue.addAll(Arrays.asList(events));
	}

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
	 * the initial AsyncContext sending the appropriate messages if necessary
	 * 
	 * @param asyncCtx
	 *            the AsynContext of the request that produced the termination
	 *            due to an explicit termination request or due to an error in
	 *            the processing of the request.
	 * @param code
	 * @param description
	 * @param requestId
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
	 * @param description
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
			if (localState == STATE.IDLE || state == STATE.HANDLING
					|| state == STATE.STARTING) {
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

	protected void destroy() {
		if (initialAsyncCtx != null) {
			initialAsyncCtx.complete();
			initialAsyncCtx = null;
		}
		manager.remove(this.sessionId);

		try {
			releaseOwnMediaServerResources();
		} catch (Throwable e) {
			getLogger().error(e.getMessage(), e);
		}
	}
	
	protected void releaseOwnMediaServerResources() throws Throwable {
		if(cleanupList == null)
			return;
		for(MediaObject mediaObject : cleanupList){
			mediaObject.release(new Continuation<Void>() {				
				@Override
				public void onSuccess(Void result) {					
				}
				
				@Override
				public void onError(Throwable cause) {
					getLogger().error(cause.getMessage(), cause);
				}
			});
		}
	}
}
