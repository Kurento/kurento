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
import com.kurento.kmf.media.HttpEndPoint.HttpEndPointBuilder;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.UriEndPoint;
import com.kurento.kmf.media.events.HttpEndPointEOSDetected;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.repository.RepositoryHttpEndpoint;
import com.kurento.kmf.repository.RepositoryItem;

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

	private final boolean terminateOnEOS;

	public HttpPlayerSessionImpl(HttpPlayerHandler handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol,
			boolean terminateOnEOS) {
		super(handler, manager, asyncContext, contentId, redirect,
				useControlProtocol);
		this.terminateOnEOS = terminateOnEOS;
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

	/**
	 * Perform a play action using a MediaElement.
	 */
	@Override
	public void start(MediaElement element) {
		try {
			Assert.notNull(element, "Illegal null source element provided",
					10028);
			activateMedia(null, new MediaElement[] { element });

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
	public void start(RepositoryItem repositoryItem) {
		try {
			Assert.notNull(repositoryItem, "Illegal null repository provided",
					10027);
			activateMedia(repositoryItem);
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
	protected UriEndPoint buildUriEndPoint(String contentPath) {
		getLogger().info("Creating media pipeline ...");
		MediaPipeline mediaPipeline = mediaPipelineFactory.create();
		releaseOnTerminate(mediaPipeline);
		getLogger().info("Creating PlayerEndPoint ...");
		PlayerEndPoint playerEndPoint = mediaPipeline.newPlayerEndPoint(
				contentPath).build();
		return playerEndPoint;
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	@Override
	protected HttpEndPoint buildAndConnectHttpEndPoint(
			MediaElement... mediaElements) {

		// In this case (player) we can connect to one media element
		// (source) that must be the first in the array. This is not very
		// beautiful but makes possible to have player and recorder on the
		// same inheritance hierarchy
		MediaElement mediaElement = mediaElements[0];
		getLogger().info("Recovering media pipeline");
		MediaPipeline mediaPiplePipeline = mediaElement.getMediaPipeline();
		getLogger().info("Creating HttpEndPoint ...");
		HttpEndPointBuilder builder = mediaPiplePipeline.newHttpEndPoint();

		if (terminateOnEOS) {
			builder.terminateOnEOS();
		}

		HttpEndPoint httpEndPoint = builder.build();

		// TODO: this listener is just for debugging purposes. Remove in the
		// future
		httpEndPoint
				.addEOSDetectedListener(new MediaEventListener<HttpEndPointEOSDetected>() {
					@Override
					public void onEvent(HttpEndPointEOSDetected event) {
						getLogger().info(
								"Received OES on HttpPlayerSessionImpl with id "
										+ getSessionId());
					}
				});

		releaseOnTerminate(httpEndPoint);
		mediaElement.connect(httpEndPoint);

		getLogger().info(
				"Adding PlayerEndPoint.play() into HttpEndPoint listener");

		return httpEndPoint;
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void interalRawCallToOnSessionTerminated(int code,
			String description) throws Exception {
		getHandler().onSessionTerminated(this, code, description);
	}

	@Override
	protected void interalRawCallToOnContentStarted() throws Exception {
		getHandler().onContentStarted(this);
	}

	@Override
	protected void interalRawCallToOnContentError(int code, String description)
			throws Exception {
		getHandler().onSessionError(this, code, description);
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

	@Override
	protected RepositoryHttpEndpoint createRepositoryHttpEndpoint(
			RepositoryItem repositoryItem) {
		return repositoryItem.createRepositoryHttpPlayer();
		// TODO: who releases this?
		// Should it be released in a per-session basis? In that case, we cannot
		// re-use if useControlProtocol = false.
	}

}
