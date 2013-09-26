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
package com.kurento.kmf.content.internal.player;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.base.AbstractHttpBasedContentSession;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.HttpEndPointEvent;
import com.kurento.kmf.media.HttpEndPointEvent.HttpEndPointEventType;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaEventListener;
import com.kurento.kmf.media.MediaException;
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
public class HttpPlayerSessionImpl extends AbstractHttpBasedContentSession
		implements HttpPlayerSession {

	private static final Logger log = LoggerFactory
			.getLogger(HttpPlayerSessionImpl.class);

	private PlayerEndPoint playerEndPoint = null;

	public HttpPlayerSessionImpl(HttpPlayerHandler handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(handler, manager, asyncContext, contentId, redirect,
				useControlProtocol);
	}

	@Override
	protected HttpPlayerHandler getHandler() {
		return (HttpPlayerHandler) super.getHandler();
	}

	@Override
	public void start(String contentPath) {
		try {
			Assert.notNull(contentPath, "Illegal null contentPath provided",
					10027);
			activateMedia(contentPath, (MediaElement[]) null);
		} catch (KurentoMediaFrameworkException ke) {
			terminate(ke.getCode(), ke.getMessage());
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20030);
			terminate(kmfe.getCode(), kmfe.getMessage());
			throw kmfe;
		}
	}

	/**
	 * Perform a play action using a MediaElement.
	 */
	@Override
	public void start(MediaElement element) {
		try {
			Assert.notNull(element, "Illegal null source element provided",
					10028);
			activateMedia(null, new MediaElement[] { element });

			// TODO: In the future we should avoid terminating upon exceptions
			// on start. This would mean to clean and make whaeterver is
			// necessary on the media server for letting everything ready for
			// calling again start without redundant media elements.
		} catch (KurentoMediaFrameworkException ke) {
			terminate(ke.getCode(), ke.getMessage());
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20031);
			terminate(kmfe.getCode(), kmfe.getMessage());
			throw kmfe;
		}
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
		}
	}

	@Override
	protected MediaElement buildRepositoryBasedMediaElement(String contentPath) {
		try {
			getLogger().info("Creating media pipeline ...");
			MediaPipeline mediaPipeline = mediaPipelineFactory
					.createMediaPipeline();
			releaseOnTerminate(mediaPipeline);
			getLogger().info("Creating PlayerEndPoint ...");
			playerEndPoint = mediaPipeline.createUriEndPoint(
					PlayerEndPoint.class, contentPath);

			return playerEndPoint;
		} catch (IOException ioe) {
			throw new KurentoMediaFrameworkException(ioe.getMessage(), ioe,
					20032);
		} catch (MediaException me) {
			throw new KurentoMediaFrameworkException(me.getMessage(), me, 20033);
		}
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	@Override
	protected HttpEndPoint buildAndConnectHttpEndPointMediaElement(
			MediaElement... mediaElements) {
		try {
			// In this case (player) we can connect to one media element
			// (source) that must be the first in the array. This is not very
			// beautiful but makes possible to have player and recorder on the
			// same inheritance hierarchy
			MediaElement mediaElement = mediaElements[0];
			getLogger().info("Recovering media pipeline");
			MediaPipeline mediaPiplePipeline = mediaElement.getMediaPipeline();
			getLogger().info("Creating HttpEndPoint ...");
			HttpEndPoint httpEndPoint = mediaPiplePipeline.createHttpEndPoint();
			releaseOnTerminate(httpEndPoint);
			connect(mediaElement, new MediaElement[] { httpEndPoint });

			if (playerEndPoint != null) {
				// Release pipeline when player ends
				// TODO: This should be done with events on activateMedia, see
				// TODO there
				playerEndPoint
						.addListener(new MediaEventListener<PlayerEvent>() {
							@Override
							public void onEvent(PlayerEvent event) {
								if (event.getType() == PlayerEventType.EOS) {
									HttpPlayerSessionImpl.this
											.callOnContentCompletedOnHandler();
								}
							}
						});
			}

			getLogger().info(
					"Adding PlayerEndPoint.play() into HttpEndPoint listener");
			// TODO: this should be done with events on activateMedia, see TODO
			// there
			httpEndPoint
					.addListener(new MediaEventListener<HttpEndPointEvent>() {

						@Override
						public void onEvent(HttpEndPointEvent event) {
							if (event.getType() != HttpEndPointEventType.GET_REQUEST) {
								String errorMessage = "Unexpected HTTP method "
										+ event.getType()
										+ " received on HttpEndPoint. Cannot start player";
								log.error(errorMessage);
								HttpPlayerSessionImpl.this.terminate(10029,
										errorMessage);
								return;
							}

							if (playerEndPoint != null) {
								try {
									playerEndPoint.play();
								} catch (IOException e) {
									String errorMessage = "Cannot invoke play on PlayerEndPoint: "
											+ e.getMessage();
									log.error(errorMessage, e);
									HttpPlayerSessionImpl.this.terminate(20034,
											errorMessage);
								}
							}
						}
					});
			return httpEndPoint;
		} catch (IOException ioe) {
			throw new KurentoMediaFrameworkException(ioe.getMessage(), ioe,
					20035);
		} catch (MediaException me) {
			throw new KurentoMediaFrameworkException(me.getMessage(), me, 20036);
		}
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void interalRawCallToOnContentCompleted() throws Exception {
		getHandler().onContentCompleted(this);
	}

	@Override
	protected void interalRawCallToOnContentStarted() throws Exception {
		getHandler().onContentStarted(this);
	}

	@Override
	protected void interalRawCallToOnContentError(int code, String description)
			throws Exception {
		getHandler().onContentError(this, code, description);
	}

	@Override
	protected void internalRawCallToOnContentRequest() throws Exception {
		getHandler().onContentRequest(this);
	}

	@Override
	protected void internalRawCallToOnUncaughtExceptionThrown(Throwable t)
			throws Exception {
		getHandler().onUncaughtException(this, t);

	}

	@Override
	protected ContentCommandResult interalRawCallToOnContentCommand(
			ContentCommand command) throws Exception {
		return getHandler().onContentCommand(this, command);
	}
}
