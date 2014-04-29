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
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.media.SdpEndpoint;
import com.kurento.kmf.media.events.ErrorEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;
import com.kurento.kmf.repository.RepositoryHttpPlayer;
import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * 
 * Extension of Content Request to support encapsulated SDP in requests.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class AbstractSdpContentSession extends AbstractContentSession
		implements SdpContentSession {

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

	private RepositoryHttpPlayer repositoryHttpPlayer;
	private RepositoryHttpRecorder repositoryHttpRecorder;

	public AbstractSdpContentSession(
			ContentHandler<? extends ContentSession> handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId) {
		super(handler, manager, asyncContext, contentId);
	}

	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) {
		Assert.notNull(
				initialJsonRequest.getParams().getSdp(),
				"SDP cannot be null on message with method "
						+ message.getMethod(), 10024);
		super.processStartJsonRpcRequest(asyncCtx, message);
	}

	/**
	 * Build an end point for SDP declaration in a MediaPipeline.
	 * 
	 * @param mediaPipeline
	 *            a MediaPipeline for which the SdpEndpoint is created
	 * @return a SdpEndpoint 
	 * @throws Throwable
	 *             Error/Exception
	 */
	protected abstract SdpEndpoint buildSdpEndpoint(MediaPipeline mediaPipeline);

	/**
	 * Star media element implementation.
	 * 
	 * @param sourceContentPath
	 *            Path of outgoing media element
	 * @param sinkContentPath
	 *            Path of ingoing media element
	 * @throws KurentoMediaFrameworkException
	 *             Exception while sending an SDP answer to client
	 */
	@Override
	public void start(String sourceContentPath, String sinkContentPath) {
		try {
			Assert.isTrue(
					sourceContentPath != null || sinkContentPath != null,
					"Cannot invoke start specifying null source and sink content paths",
					1); // TODO

			goToState(
					STATE.STARTING,
					"Cannot start SdpEndPoint in state "
							+ getState()
							+ ". This is probably due to an explicit session termination comming from another thread",
					1); // TODO

			internalStart(sourceContentPath, sinkContentPath);

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
	public void start(RepositoryItem sourceRepositoryItem,
			RepositoryItem sinkRepositoryItem) {
		try {
			Assert.isTrue(
					sourceRepositoryItem != null || sinkRepositoryItem != null,
					"Cannot invoke start specifying null source and sink content paths",
					1); // TODO

			goToState(
					STATE.STARTING,
					"Cannot start SdpEndPoint in state "
							+ getState()
							+ ". This is probably due to an explicit session termination comming from another thread",
					1); // TODO

			if (sourceRepositoryItem != null) {
				repositoryHttpPlayer = sourceRepositoryItem
						.createRepositoryHttpPlayer();
			}

			if (sinkRepositoryItem != null) {
				repositoryHttpRecorder = sinkRepositoryItem
						.createRepositoryHttpRecorder();
			}

			String sourceContentPath = sourceRepositoryItem == null ? null
					: repositoryHttpPlayer.getURL();
			String sinkContentPath = sinkRepositoryItem == null ? null
					: repositoryHttpRecorder.getURL();

			internalStart(sourceContentPath, sinkContentPath);

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

	private void internalStart(String sourceContentPath, String sinkContentPath) {
		MediaPipeline mediaPipeline = createMediaPipeline();
		PlayerEndpoint playerEndpoint = null;
		if (sourceContentPath != null) {
			playerEndpoint = createSourceEndpoint(mediaPipeline,
					sourceContentPath);
		}

		RecorderEndpoint recorderEndpoint = null;
		if (sinkContentPath != null) {
			recorderEndpoint = createSinkEndpoint(mediaPipeline,
					sinkContentPath);
		}

		SdpEndpoint sdpEndpoint = buildAndConnectSdpEndpoint(mediaPipeline,
				playerEndpoint, recorderEndpoint);

		if (playerEndpoint != null)
			playerEndpoint.play();

		if (recorderEndpoint != null)
			recorderEndpoint.record();

		activateMedia(sdpEndpoint, null); // TODO. Ask Jose if
											// playerEndpoint.play() can be
											// in the Runnable
	}

	protected void internalStart(SdpEndpoint sdpEndpoint) {
		try {
			releaseOnTerminate(sdpEndpoint);
			Assert.isTrue(sdpEndpoint != null,
					"Cannot invoke start specifying null endPoint", 1); // TODO

			goToState(
					STATE.STARTING,
					"Cannot start SdpEndPoint in state "
							+ getState()
							+ ". This is probably due to an explicit session termination comming from another thread",
					1); // TODO

			activateMedia(sdpEndpoint, null);

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

	private MediaPipeline createMediaPipeline() {
		getLogger().info("Creating media pipeline ...");
		MediaPipeline mediaPipeline = mediaPipelineFactory.create();
		releaseOnTerminate(mediaPipeline);
		return mediaPipeline;
	}

	private PlayerEndpoint createSourceEndpoint(MediaPipeline mediaPipeline,
			String contentPath) {
		getLogger().info("Creating PlayerEndpoint ...");
		PlayerEndpoint playerEndpoint = mediaPipeline.newPlayerEndpoint(
				contentPath).build();
		return playerEndpoint;
	}

	private RecorderEndpoint createSinkEndpoint(MediaPipeline mediaPipeline,
			String contentPath) {
		getLogger().info("Creating RecorderEndpoint ...");
		RecorderEndpoint recorderEndpoint = mediaPipeline.newRecorderEndpoint(
				contentPath).build();
		return recorderEndpoint;
	}

	private void activateMedia(SdpEndpoint sdpEndpoint,
			final Runnable runOnContentStart) {

		// Manage fatal errors in pipeline
		sdpEndpoint.getMediaPipeline().addErrorListener(
				new MediaEventListener<ErrorEvent>() {

					@Override
					public void onEvent(ErrorEvent error) {
						getLogger().error(error.getDescription()); // TODO
						internalTerminateWithError(null, error.getErrorCode(),
								error.getDescription(), null);
					}
				});

		// Invoke handler when content start
		sdpEndpoint
				.addMediaSessionStartedListener(new MediaEventListener<MediaSessionStartedEvent>() {

					@Override
					public void onEvent(MediaSessionStartedEvent event) {
						callOnContentStartedOnHanlder();
						if (runOnContentStart != null)
							runOnContentStart.run();
					}
				});

		// Manage end of media session
		sdpEndpoint
				.addMediaSessionTerminatedListener(new MediaEventListener<MediaSessionTerminatedEvent>() {

					@Override
					public void onEvent(MediaSessionTerminatedEvent event) {
						internalTerminateWithoutError(null, 1, "TODO", null);// TODO

					}
				});

		String answerSdp = sdpEndpoint.processOffer(initialJsonRequest
				.getParams().getSdp());
		getLogger().info("Answer SDP: " + answerSdp);
		Assert.notNull(answerSdp,
				"Received invalid null SDP from media server ... aborting",
				20027);
		Assert.isTrue(answerSdp.length() > 0,
				"Received invalid empty SDP from media server ... aborting",
				20028);

		goToState(
				STATE.ACTIVE,
				"Cannot start session in sate "
						+ getState()
						+ ". This is probably due to an explicit termination of the session from a different threaad",
				1);
		protocolManager.sendJsonAnswer(initialAsyncCtx, JsonRpcResponse
				.newStartSdpResponse(answerSdp, sessionId,
						initialJsonRequest.getId()));
		initialAsyncCtx = null;
		initialJsonRequest = null;
	}

	private SdpEndpoint buildAndConnectSdpEndpoint(MediaPipeline mediaPipeline,
			MediaElement sourceElement, MediaElement sinkElement) {

		getLogger().info("Creating SdpEndpoint ...");
		SdpEndpoint sdpEndpoint = buildSdpEndpoint(mediaPipeline);
		releaseOnTerminate(sdpEndpoint);

		// If no source is provided, jut loopback for having some media back
		// to the client
		if (sourceElement == null) {
			sourceElement = sdpEndpoint; // This produces a loopback.
		}

		getLogger().info("Connecting media pads ...");
		// TODO: should we double check constraints?
		if (sinkElement != null) {
			sdpEndpoint.connect(sinkElement);
		}

		if (sourceElement != null) {
			sourceElement.connect(sdpEndpoint);
		}

		return sdpEndpoint;
	}

	@Override
	protected synchronized void destroy() {
		super.destroy();

		if (repositoryHttpPlayer != null) {
			repositoryHttpPlayer.stop();
			repositoryHttpPlayer = null;
		}

		if (repositoryHttpRecorder != null) {
			repositoryHttpRecorder.stop();
			repositoryHttpRecorder = null;
		}

	}
}
