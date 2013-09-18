package com.kurento.kmf.content.internal.rtp;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.common.excption.internal.ExceptionUtils;
import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.RtpContentSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.base.AbstractSdpBasedMediaRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaException;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RtpEndPoint;

/**
 * 
 * Request implementation for a Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class RtpContentSessionImpl extends AbstractSdpBasedMediaRequest
		implements RtpContentSession {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(RtpContentSessionImpl.class);

	public RtpContentSessionImpl(RtpContentHandler handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId) {
		super(handler, manager, asyncContext, contentId);
	}

	@Override
	protected String buildMediaEndPointAndReturnSdp(MediaElement sourceElement,
			MediaElement... sinkElements) {

		try {
			// Candidate for providing a pipeline
			log.info("Looking for candidate ...");

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

			log.info("Creating media candidate for candidate " + candidate);
			MediaPipeline mediaPipeline = null;
			if (candidate != null) {
				mediaPipeline = candidate.getMediaPipeline();
			} else {
				mediaPipeline = mediaPipelineFactory.createMediaPipeline();
				releaseOnTerminate(mediaPipeline);
			}

			log.info("Creating rtpEndPoint ...");
			RtpEndPoint rtpEndPoint = mediaPipeline
					.createSdpEndPoint(RtpEndPoint.class);
			if (candidate != null) {
				releaseOnTerminate(rtpEndPoint);
			}

			log.info("Recoveing answer sdp ...");
			String answerSdp = rtpEndPoint.processOffer(initialJsonRequest
					.getSdp());

			// If no source is provided, jut loopback for having some media back
			// to
			// the client
			if (sourceElement == null) {
				sourceElement = rtpEndPoint; // This produces a loopback.
			}

			log.info("Connecting media pads ...");
			// TODO: should we double check constraints?
			if (sinkElements != null) {
				connect(rtpEndPoint, sinkElements);
			}

			if (sourceElement != null) {
				connect(sourceElement, new MediaElement[] { rtpEndPoint });
			}

			log.info("Returning answer sdp ...");
			return answerSdp;
		} catch (IOException ioe) {
			throw new KurentoMediaFrameworkException(ioe.getMessage(), ioe,
					20023);
		} catch (MediaException me) {
			throw new KurentoMediaFrameworkException(me.getMessage(), me, 20024);
		}
	}

	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) {
		Assert.notNull(
				initialJsonRequest.getSdp(),
				"SDP cannot be null on message with method "
						+ message.getMethod(), 10024);
		super.processStartJsonRpcRequest(asyncCtx, message);
	}

	/**
	 * Logger accessor (getter).
	 */
	@Override
	protected Logger getLogger() {
		return log;
	}

	/**
	 * Performs then sendJsonError using JSON protocol control manager.
	 */
	@Override
	protected void sendOnTerminateErrorMessageInInitialContext(int code,
			String description) {
		protocolManager.sendJsonError(initialAsyncCtx, JsonRpcResponse
				.newError(ExceptionUtils.getJsonErrorCode(code), description,
						initialJsonRequest.getId()));
	}

	@Override
	public RtpContentHandler getHandler() {
		return (RtpContentHandler) super.getHandler();
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
}
