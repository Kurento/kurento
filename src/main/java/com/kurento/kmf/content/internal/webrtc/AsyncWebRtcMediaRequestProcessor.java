package com.kurento.kmf.content.internal.webrtc;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

public class AsyncWebRtcMediaRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncWebRtcMediaRequestProcessor.class);

	public AsyncWebRtcMediaRequestProcessor(
			AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		super(contentRequest, requestMessage, asyncCtx);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void reallyRun() throws Throwable {
		getWebRtcMediaRequest().processControlMessage(asyncCtx, requestMessage);
	}

	private WebRtcMediaRequestImpl getWebRtcMediaRequest() {
		return (WebRtcMediaRequestImpl) contentRequest;
	}

}
