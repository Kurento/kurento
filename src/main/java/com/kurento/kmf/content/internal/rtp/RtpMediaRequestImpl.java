package com.kurento.kmf.content.internal.rtp;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractSdpBasedMediaRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RtpEndPoint;

public class RtpMediaRequestImpl extends AbstractSdpBasedMediaRequest implements
		RtpMediaRequest {

	private static final Logger log = LoggerFactory
			.getLogger(RtpMediaRequestImpl.class);

	private RtpMediaHandler handler;

	public RtpMediaRequestImpl(RtpMediaHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId) {
		super(manager, asyncContext, contentId);
		this.handler = handler;
	}

	@Override
	protected String buildMediaEndPointAndReturnSdp(MediaElement sinkElement,
			MediaElement sourceElement) throws Throwable {

		// Candidate for providing a pipeline
		log.info("Looking for candidate ...");
		MediaElement candidate = sinkElement == null ? sourceElement
				: sinkElement;

		// TODO we should check that, if both elements are non-null, they belong
		// to the same pipeline

		log.info("Creating media candidate for candidate " + candidate);
		MediaPipeline mediaPipeline = null;
		if (candidate != null) {
			mediaPipeline = candidate.getMediaPipeline();
		} else {
			mediaPipeline = mediaPipelineFactory.createMediaPipeline();
			addForCleanUp(mediaPipeline);
		}

		log.info("Creating rtpEndPoint ...");
		RtpEndPoint rtpEndPoint = mediaPipeline
				.createSdpEndPoint(RtpEndPoint.class);
		if (candidate != null) {
			addForCleanUp(rtpEndPoint);
		}

		log.info("Recoveing answer sdp ...");
		String answerSdp = rtpEndPoint
				.processOffer(initialJsonRequest.getSdp());

		// If both media elements are null, the rtpEndPoint will loopback
		// its media. This may be useful for testing purposes.
		if (candidate == null) {
			sinkElement = rtpEndPoint;// This produces a loopback.
		}

		log.info("Connecting media pads ...");
		// TODO: should we double check constraints?
		if (sinkElement != null) {
			connect(rtpEndPoint, sinkElement);
		}

		if (sourceElement != null) {
			connect(sourceElement, rtpEndPoint);
		}

		log.info("Returning answer sdp ...");
		return answerSdp;
	}

	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		Assert.notNull(
				initialJsonRequest.getSdp(),
				"SDP cannot be null on message with method "
						+ message.getMethod());
		handler.onMediaRequest(this);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void cancelMediaTransmission() {
		// TODO improve this
		terminate(500, "Transmission was cancelled by an external command");
	}

	@Override
	protected void sendOnTerminateErrorMessageInInitialContext(int code,
			String description) throws IOException {
		protocolManager.sendJsonError(
				initialAsyncCtx,
				JsonRpcResponse.newError(code, description,
						initialJsonRequest.getId()));

	}
}
