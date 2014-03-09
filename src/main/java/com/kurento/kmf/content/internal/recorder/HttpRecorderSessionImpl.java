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
package com.kurento.kmf.content.internal.recorder;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.HttpRecorderHandler;
import com.kurento.kmf.content.HttpRecorderSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.base.AbstractHttpContentSession;
import com.kurento.kmf.media.HttpEndpoint;
import com.kurento.kmf.media.HttpPostEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.repository.RepositoryHttpEndpoint;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * 
 * Request implementation for a Recorder.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class HttpRecorderSessionImpl extends AbstractHttpContentSession
		implements HttpRecorderSession {

	private static final Logger log = LoggerFactory
			.getLogger(HttpRecorderSessionImpl.class);

	public HttpRecorderSessionImpl(HttpRecorderHandler handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(handler, manager, asyncContext, contentId, redirect,
				useControlProtocol);
	}

	@Override
	public HttpRecorderHandler getHandler() {
		return (HttpRecorderHandler) super.getHandler();
	}

	@Override
	public void start(HttpPostEndpoint endpoint) {
		super.start(endpoint);
	}

	@Override
	public void start(String contentPath) {
		try {
			Assert.notNull(contentPath, "Illegal null contentPath provided",
					10027);

			goToState(
					STATE.STARTING,
					"Cannot start HttpRecorderSession in state "
							+ getState()
							+ ". This is probably due to an explicit session termination comming from another thread",
					1); // TODO

			getLogger().info(
					"Activating media for " + this.getClass().getSimpleName()
							+ " with contentPath " + contentPath);

			final RecorderEndpoint recorderEndpoint = buildUriEndpoint(contentPath);
			HttpEndpoint httpEndpoint = buildAndConnectHttpEndpoint(recorderEndpoint);
			recorderEndpoint.record(); // TODO. Ask Jose if this is the best
										// place for this or it should be set as
										// in the HttpPlayerSession
			activateMedia(httpEndpoint, null);

		} catch (KurentoMediaFrameworkException ke) {
			internalTerminateWithError(null, ke.getCode(), ke.getMessage(),
					null);
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20039);
			internalTerminateWithError(null, kmfe.getCode(), kmfe.getMessage(),
					null);
			throw kmfe;
		}
	}

	/**
	 * Creates a Media Element repository using a ContentPath.
	 * 
	 */
	protected RecorderEndpoint buildUriEndpoint(String contentPath) {
		getLogger().info("Creating media pipeline ...");
		MediaPipeline mediaPipeline = mediaPipelineFactory.create();
		releaseOnTerminate(mediaPipeline);
		getLogger().info("Creating RecorderEndpoint ...");
		RecorderEndpoint recorderEndpoint = mediaPipeline.newRecorderEndpoint(
				contentPath).build();
		return recorderEndpoint;
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	protected HttpEndpoint buildAndConnectHttpEndpoint(
			RecorderEndpoint recorderEndpoint) {

		MediaPipeline mediaPiplePipeline = recorderEndpoint.getMediaPipeline();
		getLogger().info("Creating HttpEndpoint ...");
		HttpPostEndpoint httpEndpoint = mediaPiplePipeline
				.newHttpPostEndpoint().build();
		releaseOnTerminate(httpEndpoint);
		httpEndpoint.connect(recorderEndpoint);
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
		return repositoryItem.createRepositoryHttpRecorder();
	}
}
