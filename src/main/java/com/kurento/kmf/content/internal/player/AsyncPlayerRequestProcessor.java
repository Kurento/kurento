package com.kurento.kmf.content.internal.player;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;

public class AsyncPlayerRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncPlayerRequestProcessor.class);

	public AsyncPlayerRequestProcessor(AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		super(contentRequest, requestMessage, asyncCtx);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void reallyRun() throws Throwable {
		if (getPlayRequest().useControlProtocol()) {
			getPlayRequest().processControlMessage(asyncCtx, requestMessage);
		} else {
			getPlayRequest().getHandler().onPlayRequest(getPlayRequest());
		}
	}

	private PlayRequestImpl getPlayRequest() {
		return (PlayRequestImpl) contentRequest;
	}
}
