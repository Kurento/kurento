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
import com.kurento.kmf.content.internal.base.AbstractHttpContentSession;
import com.kurento.kmf.media.HttpEndpoint;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.repository.RepositoryHttpEndpoint;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * 
 * Request implementation for a Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class HttpPlayerSessionImpl extends AbstractHttpContentSession implements
		HttpPlayerSession {

	private static final Logger log = LoggerFactory
			.getLogger(HttpPlayerSessionImpl.class);

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
	public void start(HttpGetEndpoint endpoint) {
		super.start(endpoint);
	}

	@Override
	public void start(String contentPath) {
		try {
			Assert.notNull(contentPath, "Illegal null contentPath provided",
					10027);

			goToState(
					STATE.STARTING,
					"Cannot start HttpPlayerSession in state "
							+ getState()
							+ ". This is probably due to an explicit session termination comming from another thread",
					1); // TODO

			getLogger().info(
					"Activating media for " + this.getClass().getSimpleName()
							+ " with contentPath " + contentPath);

			final PlayerEndpoint playerEndPoint = buildUriEndpoint(contentPath);
			HttpEndpoint httpEndpoint = buildAndConnectHttpEndpoint(playerEndPoint);

			activateMedia(httpEndpoint, new Runnable() {
				@Override
				public void run() {
					playerEndPoint.play();

				}
			});
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

	protected PlayerEndpoint buildUriEndpoint(String contentPath) {
		getLogger().info("Creating media pipeline ...");
		MediaPipeline mediaPipeline = mediaPipelineFactory.create();
		releaseOnTerminate(mediaPipeline);
		getLogger().info("Creating PlayerEndpoint ...");
		PlayerEndpoint playerEndpoint = mediaPipeline.newPlayerEndpoint(
				contentPath).build();
		return playerEndpoint;
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	protected HttpEndpoint buildAndConnectHttpEndpoint(
			PlayerEndpoint playerEndpoint) {

		getLogger().info("Recovering media pipeline");
		MediaPipeline mediaPiplePipeline = playerEndpoint.getMediaPipeline();
		getLogger().info("Creating HttpEndpoint ...");

		HttpEndpoint httpEndpoint = mediaPiplePipeline.newHttpGetEndpoint()
				.terminateOnEOS().build();
		releaseOnTerminate(httpEndpoint);
		playerEndpoint.connect(httpEndpoint);
		return httpEndpoint;
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
	}
}
