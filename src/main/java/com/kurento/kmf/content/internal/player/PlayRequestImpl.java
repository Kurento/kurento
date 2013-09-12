package com.kurento.kmf.content.internal.player;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractHttpBasedContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.HttpEndPointEvent;
import com.kurento.kmf.media.HttpEndPointEvent.HttpEndPointEventType;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaEventListener;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.PlayerEvent;
import com.kurento.kmf.media.PlayerEvent.PlayerEventType;

/**
 * 
 * Request implementation for a Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class PlayRequestImpl extends AbstractHttpBasedContentRequest implements
		PlayRequest {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(PlayRequestImpl.class);

	/**
	 * Player Handler reference.
	 */
	private PlayerHandler handler;

	private PlayerEndPoint playerEndPoint = null;
	private HttpEndPoint httpEndPoint = null;

	/**
	 * Parameterized constructor.
	 * 
	 * @param handler
	 *            Player Handler
	 * @param manager
	 *            Content Request Manager
	 * @param asyncContext
	 *            Asynchronous context
	 * @param contentId
	 *            Content identifier
	 * @param redirect
	 *            Redirect strategy
	 * @param useControlProtocol
	 *            JSON-based signaling protocol strategy
	 */
	public PlayRequestImpl(PlayerHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(manager, asyncContext, contentId, redirect, useControlProtocol);
		this.handler = handler;
	}

	/**
	 * Player handler accessor (getter).
	 * 
	 * @return Player handler
	 */
	public PlayerHandler getHandler() {
		return handler;
	}

	/**
	 * Perform play action using a ContentPath.
	 */
	@Override
	public void play(String contentPath) throws ContentException {
		activateMedia(null, contentPath);

	}

	/**
	 * Perform a play action using a MediaElement.
	 */
	@Override
	public void play(MediaElement element) throws ContentException {
		activateMedia(element, null);
	}

	/**
	 * Creates a Media Element repository using a ContentPath.
	 */
	@Override
	public void usePlayer(PlayerEndPoint player) {
		// TODO: this is an ugly work-aroud of the problem of starting the
		// player only when the HTTP GET is received on the end-point. It has
		// several problems including the fact that the internal player can be
		// overriden
		if (player != null) {
			this.playerEndPoint = player;
			// Release pipeline when player ends
			// TODO: This should be done when GET finishes instead of using
			// player
			// event
			playerEndPoint.addListener(new MediaEventListener<PlayerEvent>() {
				@Override
				public void onEvent(PlayerEvent event) {
					if (event.getType() == PlayerEventType.EOS) {
						PlayRequestImpl.this.handler
								.onContentPlayed(PlayRequestImpl.this);
						PlayRequestImpl.this.terminate(200, "OK");
					}
				}
			});
		}
	}

	@Override
	protected MediaElement buildRepositoryBasedMediaElement(String contentPath)
			throws Exception {
		getLogger().info("Creating media pipeline ...");
		MediaPipeline mediaPipeline = mediaPipelineFactory
				.createMediaPipeline();
		addForCleanUp(mediaPipeline);
		getLogger().info("Creating PlayerEndPoint ...");
		playerEndPoint = mediaPipeline.createUriEndPoint(PlayerEndPoint.class,
				contentPath);

		// Release pipeline when player ends
		// TODO: This should be done when GET finishes instead of using player
		// event
		playerEndPoint.addListener(new MediaEventListener<PlayerEvent>() {
			@Override
			public void onEvent(PlayerEvent event) {
				if (event.getType() == PlayerEventType.EOS) {
					PlayRequestImpl.this.handler
							.onContentPlayed(PlayRequestImpl.this);
					PlayRequestImpl.this.terminate(200, "OK");
				}
			}
		});

		playerEndPoint.play();
		return playerEndPoint;
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	@Override
	protected HttpEndPoint buildAndConnectHttpEndPointMediaElement(
			MediaElement mediaElement) throws Exception {
		getLogger().info("Recovering media pipeline");
		MediaPipeline mediaPiplePipeline = mediaElement.getMediaPipeline();
		getLogger().info("Creating HttpEndPoint ...");
		httpEndPoint = mediaPiplePipeline.createHttpEndPoint();
		addForCleanUp(httpEndPoint);
		connect(mediaElement, httpEndPoint);
		getLogger().info("Adding PlayerEndPoint.play() into HttpEndPoint listener");

		httpEndPoint.addListener(new MediaEventListener<HttpEndPointEvent>() {

			@Override
			public void onEvent(HttpEndPointEvent event) {
				if (event.getType() != HttpEndPointEventType.GET_REQUEST) {
					log.error("Unexpected HTTP method " + event.getType()
							+ " received on HttpEndPoint. Cannot start player");
					PlayRequestImpl.this.terminate(
							500,
							"Unexpected HTTP method "
									+ event.getType()
									+ " received on HttpEndPoint. Cannot start player");
					return;
				}

				if (playerEndPoint != null) {
					try {
						playerEndPoint.play();
					} catch (IOException e) {
						log.error(
								"Cannot invoke play on PlayerEndPoint: "
										+ e.getMessage(), e);
						PlayRequestImpl.this.terminate(500, e.getMessage());
					}
				}
			}
		});
		return httpEndPoint;
	}

	/**
	 * Performs then onPlayeRequest event of the Handler.
	 */
	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		handler.onPlayRequest(this);
	}

	/**
	 * Logger accessor (getter).
	 */
	@Override
	protected Logger getLogger() {
		return log;
	}

	/**
	 * Cancel of media transmission.
	 */
	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub
	}
}
