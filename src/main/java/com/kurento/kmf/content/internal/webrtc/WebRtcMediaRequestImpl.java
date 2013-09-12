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

/**
 * 
 * Request implementation for WebRTC.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class WebRtcMediaRequestImpl extends AbstractSdpBasedMediaRequest
		implements WebRtcMediaRequest {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(WebRtcMediaRequestImpl.class);

	/**
	 * WebRTC Handler reference.
	 */
	private WebRtcMediaHandler handler;

	/**
	 * Parameterized constructor.
	 * 
	 * @param handler
	 *            WebRTC Handler
	 * @param manager
	 *            Content Request Manager
	 * @param asyncContext
	 *            Asynchronous context
	 * @param contentId
	 *            Content identifier
	 */
	public WebRtcMediaRequestImpl(WebRtcMediaHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId) {
		super(manager, asyncContext, contentId);
		this.handler = handler;
	}

	/**
	 * Build a Media End Point, and creates a SDP answer.
	 * 
	 * @param sinkElement
	 *            Out-going media element
	 * @param sourceElement
	 *            In-going media element
	 * @return SDP answer
	 */
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

	/**
	 * Logger accessor (getter).
	 */
	@Override
	protected Logger getLogger() {
		return log;
	}

	/**
	 * Cancel of media transmission.
	 */
	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub
	}

	/**
	 * Performs then onMediaRequest event of the Handler.
	 */
	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		Assert.notNull(
				initialJsonRequest.getSdp(),
				"SDP cannot be null on message with method "
						+ message.getMethod());
		handler.onMediaRequest(this);
	}

	/**
	 * Performs then sendJsonError using JSON protocol control manager.
	 */
	@Override
	protected void sendOnTerminateErrorMessageInInitialContext(int code,
			String description) throws IOException {
		protocolManager.sendJsonError(
				initialAsyncCtx,
				JsonRpcResponse.newError(code, description,
						initialJsonRequest.getId()));
	}
}
