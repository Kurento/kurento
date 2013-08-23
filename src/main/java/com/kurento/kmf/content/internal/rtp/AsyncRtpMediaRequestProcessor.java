package com.kurento.kmf.content.internal.rtp;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

public class AsyncRtpMediaRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncRtpMediaRequestProcessor.class);

	public AsyncRtpMediaRequestProcessor(AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		super(contentRequest, requestMessage, asyncCtx);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void reallyRun() throws Throwable {
		getRtpMediaRequest().processControlMessage(asyncCtx, requestMessage);
	}

	private RtpMediaRequestImpl getRtpMediaRequest() {
		return (RtpMediaRequestImpl) contentRequest;
	}

}
