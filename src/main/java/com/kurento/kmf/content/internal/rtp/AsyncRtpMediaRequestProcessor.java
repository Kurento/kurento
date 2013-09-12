package com.kurento.kmf.content.internal.rtp;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

/**
 * 
 * Asynchronous processor for RTP.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class AsyncRtpMediaRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(AsyncRtpMediaRequestProcessor.class);

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
	public AsyncRtpMediaRequestProcessor(AbstractContentRequest contentRequest,
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
	 * Actual implementation for thread execution for RTP.
	 */
	@Override
	protected void reallyRun() throws Throwable {
		getRtpMediaRequest().processControlMessage(asyncCtx, requestMessage);
	}

	/**
	 * RTP Request accessor (getter).
	 * 
	 * @return RTP Request object
	 */
	private RtpMediaRequestImpl getRtpMediaRequest() {
		return (RtpMediaRequestImpl) contentRequest;
	}

}
