package com.kurento.kmf.content.internal.base;

import javax.servlet.AsyncContext;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.content.ContentHandler;
import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.content.SdpContentSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
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
		AbstractContentSession implements SdpContentSession {

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
	public AbstractSdpBasedMediaRequest(
			ContentHandler<? extends ContentSession> handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId) {
		super(handler, manager, asyncContext, contentId);
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
			MediaElement sourceElement, MediaElement... sinkElements);

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
	@Override
	public void start(MediaElement sourceElement, MediaElement... sinkElements) {
		try {
			internalStart(sourceElement, sinkElements);
		} catch (KurentoMediaFrameworkException ke) {
			terminate(ke.getCode(), ke.getMessage());
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 1);// TODO: code
			terminate(kmfe.getCode(), kmfe.getMessage());
			throw kmfe;
		}
	}

	@Override
	public void start(MediaElement sourceElement, MediaElement sinkElement) {
		if (sinkElement == null) {
			start(sourceElement, (MediaElement[]) null);
		} else {
			start(sourceElement, new MediaElement[] { sinkElement });
		}
	}

	private void internalStart(MediaElement sourceElement,
			MediaElement... sinkElements) {
		synchronized (this) {
			Assert.isTrue(state == STATE.HANDLING,
					"Cannot start media exchange in state " + state
							+ ". This error means ...", 10004); // TODO further
			// explanation
			state = STATE.STARTING;
		}

		getLogger().info("SDP received " + initialJsonRequest.getSdp());

		String answer = null;

		answer = buildMediaEndPointAndReturnSdp(sourceElement, sinkElements);

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
		// Send SDP as answer to client
		getLogger().info("Answer SDP: " + answer);
		Assert.notNull(answer,
				"Received invalid null SDP from media server ... aborting", 1);// TODO:
																				// code
		Assert.isTrue(answer.length() > 0,
				"Received invalid empty SDP from media server ... aborting", 1);// TODO:
																				// code
		protocolManager.sendJsonAnswer(initialAsyncCtx, JsonRpcResponse
				.newStartSdpResponse(answer, sessionId,
						initialJsonRequest.getId()));
		initialAsyncCtx = null;
		initialJsonRequest = null;
	}

}
