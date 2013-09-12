package com.kurento.kmf.content.internal.base;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.springframework.util.Assert;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;

/**
 * 
 * Extension of Content Request to support encapsulated SDP in requests.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class AbstractSdpBasedMediaRequest extends
		AbstractContentRequest {

	/**
	 * Parameterized constructor; initial state here is HANDLING.
	 * 
	 * @param manager
	 *            Content request manager
	 * @param asyncContext
	 *            Asynchronous context
	 * @param contentId
	 *            Content identifier
	 */
	public AbstractSdpBasedMediaRequest(ContentRequestManager manager,
			AsyncContext asyncContext, String contentId) {
		super(manager, asyncContext, contentId);
	}

	/**
	 * Build end point for SDP declaration.
	 * 
	 * @param sinkElement
	 *            Out-going media element
	 * @param sourceElement
	 *            In-going media element
	 * @return answer
	 * @throws Throwable
	 *             Error/Exception
	 */
	protected abstract String buildMediaEndPointAndReturnSdp(
			MediaElement sinkElement, MediaElement sourceElement)
			throws Throwable;

	/**
	 * Star media element implementation.
	 * 
	 * @param sinkElement
	 *            Out-going media element
	 * @param sourceElement
	 *            In-going media element
	 * @throws ContentException
	 *             Exception while sending SDP as answer to client
	 */
	public void startMedia(MediaElement sinkElement, MediaElement sourceElement)
			throws ContentException {
		synchronized (this) {
			Assert.isTrue(state == STATE.HANDLING,
					"Cannot start media exchange in state " + state
							+ ". This error means ..."); // TODO further
															// explanation
			state = STATE.STARTING;
		}

		getLogger().info("SDP received " + initialJsonRequest.getSdp());

		String answer = null;

		try {
			answer = buildMediaEndPointAndReturnSdp(sinkElement, sourceElement);
		} catch (Throwable t) {
			// TODO when final KMS version is ready, perhaps it will be
			// necessary to release httpEndPoint and playerEndPoint resources.
			getLogger().error(t.getMessage(), t);
			throw new ContentException(t);
		}

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
			// TODO
			// clean up
			// return
		}
		// If session was not rejected (state=ACTIVE) we send an answer and
		// the initialAsyncCtx becomes useless
		try {
			// Send SDP as answer to client
			getLogger().info("Answer SDP: " + answer);
			Assert.notNull(answer,
					"Received invalid null SDP from media server ... aborting");
			Assert.isTrue(answer.length() > 0,
					"Received invalid empty SDP from media server ... aborting");
			protocolManager.sendJsonAnswer(initialAsyncCtx, JsonRpcResponse
					.newStartSdpResponse(answer, sessionId,
							initialJsonRequest.getId()));
			initialAsyncCtx = null;
			initialJsonRequest = null;
		} catch (IOException e) {
			// TODO when final KMS version is ready, perhaps it will be
			// necessary to release httpEndPoint and playerEndPoint
			// resources.
			throw new ContentException(e);
		}
	}
}
