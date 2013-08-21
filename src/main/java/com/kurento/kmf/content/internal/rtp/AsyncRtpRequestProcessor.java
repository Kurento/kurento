package com.kurento.kmf.content.internal.rtp;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;

public class AsyncRtpRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	public AsyncRtpRequestProcessor(AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		super(contentRequest, requestMessage, asyncCtx);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void reallyRun() throws Throwable {
		// TODO Auto-generated method stub

	}

}
