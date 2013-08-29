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

public class PlayRequestImpl extends AbstractHttpBasedContentRequest implements
		PlayRequest {

	private static final Logger log = LoggerFactory
			.getLogger(PlayRequestImpl.class);

	private PlayerHandler handler;

	private PlayerEndPoint playerEndPoint = null;

	public PlayRequestImpl(PlayerHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(manager, asyncContext, contentId, redirect, useControlProtocol);
		this.handler = handler;
	}

	public PlayerHandler getHandler() {
		return handler;
	}

	@Override
	public void play(String contentPath) throws ContentException {
		activateMedia(null, contentPath);

	}

	@Override
	public void play(MediaElement element) throws ContentException {
		activateMedia(element, null);
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

		return playerEndPoint;
	}

	@Override
	protected HttpEndPoint buildAndConnectHttpEndPointMediaElement(
			MediaElement mediaElement) throws Exception {
		MediaPipeline mediaPiplePipeline = mediaElement.getMediaPipeline();
		getLogger().info("Creating HttpEndPoint ...");
		HttpEndPoint httpEndPoint = mediaPiplePipeline.createHttpEndPoint();
		addForCleanUp(httpEndPoint);
		connect(mediaElement, httpEndPoint);
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

	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		handler.onPlayRequest(this);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub
	}
}
