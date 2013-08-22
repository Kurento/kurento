package com.kurento.kmf.content.internal.rtp;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.kurento.kmf.content.Constraints;
import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractSdpBasedMediaRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaException;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPad;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSrc;
import com.kurento.kmf.media.RtpEndPoint;
import com.kurento.kms.api.MediaType;

public class RtpMediaRequestImpl extends AbstractSdpBasedMediaRequest implements
		RtpMediaRequest {

	private static final Logger log = LoggerFactory
			.getLogger(RtpMediaRequestImpl.class);

	@Autowired
	private MediaPipelineFactory mediaPipelineFactory;

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
		MediaElement candidate = sinkElement == null ? sourceElement
				: sinkElement;

		// TODO we should check that, if both elements are non-null, they belong
		// to the same pipeline

		MediaPipeline mediaPipeline = null;
		if (candidate != null) {
			mediaPipeline = candidate.getMediaPipeline();
		} else {
			mediaPipeline = mediaPipelineFactory.createMediaPipeline();
			addForCleanUp(mediaPipeline);
		}

		RtpEndPoint rtpEndPoint = mediaPipeline
				.createSdpEndPoint(RtpEndPoint.class);
		if (candidate != null) {
			addForCleanUp(rtpEndPoint);
		}

		String answerSdp = rtpEndPoint
				.processOffer(initialJsonRequest.getSdp());

		// If both media elements are null, the rtpEndPoint will loopback
		// its media. This may be useful for testing purposes.
		if (candidate == null) {
			sinkElement = rtpEndPoint;// This produces a loopback.
		}

		// TODO: should we double check constraints?
		if (sinkElement != null) {
			connect(rtpEndPoint, sinkElement);
		}

		if (sourceElement != null) {
			connect(sourceElement, rtpEndPoint);
		}

		return answerSdp;
	}

	private void connect(MediaElement sourceElement, MediaElement sinkElement)
			throws IOException {
		MediaSink videoSink = sinkElement.getMediaSinks(MediaType.VIDEO)
				.iterator().next();
		sourceElement.getMediaSrcs(MediaType.VIDEO).iterator().next()
				.connect(videoSink);
		MediaSink audioSink = sinkElement.getMediaSinks(MediaType.AUDIO)
				.iterator().next();
		sourceElement.getMediaSrcs(MediaType.AUDIO).iterator().next()
				.connect(audioSink);
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

	@Override
	public MediaPipelineFactory getMediaPipelineFactory() {
		// TODO: this returned class should be a wrapper of the real class so
		// that when the user creates a resource the request stores the resource
		// for later cleanup
		return mediaPipelineFactory;
	}
}
