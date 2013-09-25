package com.kurento.kmf.content.internal.recorder;

import java.io.IOException;

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
import com.kurento.kmf.media.MediaException;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RecorderEndPoint;

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
			terminate(ke.getCode(), ke.getMessage());
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20039);
			terminate(kmfe.getCode(), kmfe.getMessage());
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
			terminate(ke.getCode(), ke.getMessage());
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20040);
			terminate(kmfe.getCode(), kmfe.getMessage());
			throw kmfe;
		}
	}

	@Override
	public void start(MediaElement sink) {
		try {
			Assert.notNull(sink, "Illegal null sink element specified", 10030);
			start(new MediaElement[] { sink });
		} catch (KurentoMediaFrameworkException ke) {
			terminate(ke.getCode(), ke.getMessage());
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20041);
			terminate(kmfe.getCode(), kmfe.getMessage());
			throw kmfe;
		}
	}

	/**
	 * Creates a Media Element repository using a ContentPath.
	 * 
	 */
	@Override
	protected MediaElement buildRepositoryBasedMediaElement(String contentPath) {
		getLogger().info("Creating media pipeline ...");
		try {
			MediaPipeline mediaPipeline = mediaPipelineFactory
					.createMediaPipeline();
			releaseOnTerminate(mediaPipeline);
			getLogger().info("Creating RecorderEndPoint ...");
			RecorderEndPoint recorderEndPoint = mediaPipeline
					.createUriEndPoint(RecorderEndPoint.class, contentPath);
			recorderEndPoint.record();
			return recorderEndPoint;
		} catch (IOException ioe) {
			throw new KurentoMediaFrameworkException(ioe.getMessage(), ioe,
					20042);
		} catch (MediaException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 20043);
		}
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	@Override
	protected HttpEndPoint buildAndConnectHttpEndPointMediaElement(
			MediaElement... mediaElements) {

		try {
			MediaPipeline mediaPiplePipeline = mediaElements[0]
					.getMediaPipeline();
			getLogger().info("Creating HttpEndPoint ...");
			HttpEndPoint httpEndPoint = mediaPiplePipeline.createHttpEndPoint();
			releaseOnTerminate(httpEndPoint);
			connect(httpEndPoint, mediaElements);
			return httpEndPoint;
		} catch (IOException ioe) {
			throw new KurentoMediaFrameworkException(ioe.getMessage(), ioe,
					20037);
		} catch (MediaException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 20038);
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
