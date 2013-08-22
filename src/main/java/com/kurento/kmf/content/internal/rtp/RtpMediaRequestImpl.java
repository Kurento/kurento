package com.kurento.kmf.content.internal.rtp;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractSdpBasedMediaRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;
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
	protected String buildMediaEndPointAndReturnSdp(MediaElement upStream,
			MediaElement downStream) throws Throwable {

		MediaPipeline mediaPipeline = mediaPipelineFactory
				.createMediaPipeline();
		addForCleanUp(mediaPipeline);

		RtpEndPoint rtpEndPoint = mediaPipeline
				.createSdpEndPoint(RtpEndPoint.class);
		MediaSrc videoSource = rtpEndPoint.getMediaSrcs(MediaType.VIDEO)
				.iterator().next();
		MediaSink videoSink = rtpEndPoint.getMediaSinks(MediaType.VIDEO)
				.iterator().next();
		videoSource.connect(videoSink);

		MediaSrc audioSource = rtpEndPoint.getMediaSrcs(MediaType.AUDIO)
				.iterator().next();
		MediaSink audioSink = rtpEndPoint.getMediaSinks(MediaType.AUDIO)
				.iterator().next();
		audioSource.connect(audioSink);

		String answerSdp = rtpEndPoint
				.processOffer(initialJsonRequest.getSdp());
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
