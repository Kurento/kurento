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

import javax.servlet.AsyncContext;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.content.ContentHandler;
import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.content.SdpContentSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.SdpEndPoint;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaErrorListener;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;

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
	protected abstract SdpEndPoint buildSdpEndPoint(MediaPipeline mediaPipeline);

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
			internalTerminateWithError(null, ke.getCode(), ke.getMessage(),
					null);
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20029);
			internalTerminateWithError(null, kmfe.getCode(), kmfe.getMessage(),
					null);
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
			Assert.isTrue(
					state == STATE.HANDLING,
					"Cannot start media exchange in state "
							+ state
							+ ". This error means a violatiation in the content session lifecycle",
					10004);
			state = STATE.STARTING;
		}

		getLogger().info(
				"SDP received " + initialJsonRequest.getParams().getSdp());

		SdpEndPoint sdpEndPoint = buildAndConnectSdpEndPoint(sourceElement,
				sinkElements);

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
			destroy(); // idempotent call. Just in case pipeline gets build
						// after session executes termination
			return;
		}

		// Manage fatal errors in pipeline
		sdpEndPoint.getMediaPipeline().addErrorListener(
				new MediaErrorListener() {

					@Override
					public void onError(MediaError error) {
						getLogger().error(error.getDescription()); // TODO
						internalTerminateWithError(null, error.getErrorCode(),
								error.getDescription(), null);
					}
				});

		// Invoke handler when content start
		sdpEndPoint
				.addMediaSessionStartListener(new MediaEventListener<MediaSessionStartedEvent>() {

					@Override
					public void onEvent(MediaSessionStartedEvent event) {
						callOnContentStartedOnHanlder();
					}
				});

		// Manage end of media session
		sdpEndPoint
				.addMediaSessionCompleteListener(new MediaEventListener<MediaSessionTerminatedEvent>() {

					@Override
					public void onEvent(MediaSessionTerminatedEvent event) {
						internalTerminateWithoutError(null, 1, "TODO", null);// TODO

					}
				});

		String answerSdp = sdpEndPoint.processOffer(initialJsonRequest
				.getParams().getSdp());

		// If session was not rejected (state=ACTIVE) we send an answer and
		// the initialAsyncCtx becomes useless
		// Send SDP as answer to client
		getLogger().info("Answer SDP: " + answerSdp);
		Assert.notNull(answerSdp,
				"Received invalid null SDP from media server ... aborting",
				20027);
		Assert.isTrue(answerSdp.length() > 0,
				"Received invalid empty SDP from media server ... aborting",
				20028);
		protocolManager.sendJsonAnswer(initialAsyncCtx, JsonRpcResponse
				.newStartSdpResponse(answerSdp, sessionId,
						initialJsonRequest.getId()));
		initialAsyncCtx = null;
		initialJsonRequest = null;
	}

	private SdpEndPoint buildAndConnectSdpEndPoint(MediaElement sourceElement,
			MediaElement[] sinkElements) {
		// Candidate for providing a pipeline
		getLogger().info("Looking for candidate ...");

		if (sinkElements != null && sinkElements.length > 0) {
			for (MediaElement e : sinkElements) {
				Assert.notNull(e, "Illegal null sink provided", 10023);
			}
		}

		MediaElement candidate = null;
		if (sinkElements == null || sinkElements.length == 0) {
			candidate = sourceElement;
		} else {
			candidate = sinkElements[0];
		}

		getLogger().info("Creating media candidate for candidate " + candidate);
		MediaPipeline mediaPipeline = null;
		if (candidate != null) {
			mediaPipeline = candidate.getMediaPipeline();
		} else {
			mediaPipeline = mediaPipelineFactory.create();
			releaseOnTerminate(mediaPipeline);
		}

		getLogger().info("Creating rtpEndPoint ...");
		SdpEndPoint sdpEndPoint = buildSdpEndPoint(mediaPipeline);
		releaseOnTerminate(sdpEndPoint);

		// If no source is provided, jut loopback for having some media back
		// to the client
		if (sourceElement == null) {
			sourceElement = sdpEndPoint; // This produces a loopback.
		}

		getLogger().info("Connecting media pads ...");
		// TODO: should we double check constraints?
		if (sinkElements != null) {
			connect(sdpEndPoint, sinkElements);
		}

		if (sourceElement != null) {
			sourceElement.connect(sdpEndPoint);
		}

		return sdpEndPoint;
	}

}
