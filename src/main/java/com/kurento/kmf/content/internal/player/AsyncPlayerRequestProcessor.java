package com.kurento.kmf.content.internal.player;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

/**
 * 
 * Asynchronous processor for Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class AsyncPlayerRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(AsyncPlayerRequestProcessor.class);

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
	public AsyncPlayerRequestProcessor(AbstractContentRequest contentRequest,
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
	 * Actual implementation for thread execution for a player.
	 */
	@Override
	protected void reallyRun() throws Throwable {
		if (getPlayRequest().useControlProtocol()) {
			getPlayRequest().processControlMessage(asyncCtx, requestMessage);
		} else {
			getPlayRequest().getHandler().onPlayRequest(getPlayRequest());
		}
	}

	/**
	 * Play Request accessor (getter).
	 * 
	 * @return Play Request object
	 */
	private PlayRequestImpl getPlayRequest() {
		return (PlayRequestImpl) contentRequest;
	}
}
