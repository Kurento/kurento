package com.kurento.kmf.content.internal.webrtc;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.WebRtcMediaRequest;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractSdpBasedMediaRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;

public class WebRtcMediaRequestImpl extends AbstractSdpBasedMediaRequest
		implements WebRtcMediaRequest {

	private static final Logger log = LoggerFactory
			.getLogger(WebRtcMediaRequestImpl.class);

	private WebRtcMediaHandler handler;

	public WebRtcMediaRequestImpl(WebRtcMediaHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId) {
		super(manager, asyncContext, contentId);
		this.handler = handler;
	}

	@Override
	protected String buildMediaEndPointAndReturnSdp(MediaElement sinkElement,
			MediaElement sourceElement) throws Throwable {
		// TODO Create WebRtcEndPoint
		// TODO Store media elements for later clean-up
		// TODO Provide SDP to WebRtcEndPoint
		// TODO connect endpoint to provided MediaElements
		// TODO send SDP as answer to client
		// TODO blocking calls here should be interruptible

		// TODO: This answer is temporary, for debugging purposes (returning the
		// same
		// SDP received)
		return initialJsonRequest.getSdp();
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub
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
	protected void sendOnTerminateErrorMessageInInitialContext(int code,
			String description) throws IOException {
		protocolManager.sendJsonError(
				initialAsyncCtx,
				JsonRpcResponse.newError(code, description,
						initialJsonRequest.getId()));
	}
}
