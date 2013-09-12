package com.kurento.kmf.content.internal.webrtc;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

/**
 * 
 * Asynchronous processor for WebRTC.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class AsyncWebRtcMediaRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(AsyncWebRtcMediaRequestProcessor.class);

	/**
	 * Parameterized constructor.
	 * 
	 * @param contentRequest
	 *            Content request
	 * @param requestMessage
	 *            JSON RPC message
	 * @param asyncCtx
	 *            Asynchronous context
	 */
	public AsyncWebRtcMediaRequestProcessor(
			AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		super(contentRequest, requestMessage, asyncCtx);
	}

	/**
	 * Logger accessor (getter).
	 */
	@Override
	protected Logger getLogger() {
		return log;
	}

	/**
	 * Actual implementation for thread execution for WebRTC.
	 */
	@Override
	protected void reallyRun() throws Throwable {
		getWebRtcMediaRequest().processControlMessage(asyncCtx, requestMessage);
	}

	/**
	 * WebRTC Request accessor (getter).
	 * 
	 * @return WebRTC Request object
	 */
	private WebRtcMediaRequestImpl getWebRtcMediaRequest() {
		return (WebRtcMediaRequestImpl) contentRequest;
	}

}
