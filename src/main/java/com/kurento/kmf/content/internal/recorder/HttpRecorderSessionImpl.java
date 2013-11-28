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
import com.kurento.kmf.content.internal.base.AbstractHttpBasedContentSession;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RecorderEndPoint;
import com.kurento.kmf.media.UriEndPoint;

/**
 * 
 * Request implementation for a Recorder.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class HttpRecorderSessionImpl extends AbstractHttpBasedContentSession
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
	public void start(String contentPath) {
		try {
			Assert.notNull(contentPath, "Illegal null contentPath specified",
					10016);
			activateMedia(contentPath, (MediaElement[]) null);
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
	 * Perform a record action using a MediaElement.
	 * 
	 * @param element
	 *            Pluggable media component
	 */
	@Override
	public void start(MediaElement... elements) {
		try {
			Assert.notNull(elements, "Illegal null sink elements specified",
					10017);
			Assert.isTrue(elements.length > 0,
					"Illegal empty array of sink elements specified", 10018);
			for (MediaElement e : elements) {
				Assert.notNull(e,
						"Illegal null sink element specified within array",
						10019);
			}
			activateMedia(null, elements);
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
	public void start(MediaElement sink) {
		try {
			Assert.notNull(sink, "Illegal null sink element specified", 10030);
			start(new MediaElement[] { sink });
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
	 * Creates a Media Element repository using a ContentPath.
	 * 
	 */
	@Override
	protected UriEndPoint buildUriEndPoint(String contentPath) {
		getLogger().info("Creating media pipeline ...");

		MediaPipeline mediaPipeline = mediaPipelineFactory.create();
		releaseOnTerminate(mediaPipeline);
		getLogger().info("Creating RecorderEndPoint ...");
		RecorderEndPoint recorderEndPoint = mediaPipeline.newRecorderEndPoint(
				contentPath).build();
		return recorderEndPoint;
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	@Override
	protected HttpEndPoint buildAndConnectHttpEndPoint(
			MediaElement... mediaElements) {

		MediaPipeline mediaPiplePipeline = mediaElements[0].getMediaPipeline();
		getLogger().info("Creating HttpEndPoint ...");
		HttpEndPoint httpEndPoint = mediaPiplePipeline.newHttpEndPoint()
				.build();
		releaseOnTerminate(httpEndPoint);
		connect(httpEndPoint, mediaElements);
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
}
