package com.kurento.kmf.content.internal.recorder;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

public class AsyncRecorderRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncRecorderRequestProcessor.class);

	public AsyncRecorderRequestProcessor(AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		super(contentRequest, requestMessage, asyncCtx);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void reallyRun() throws Throwable {
		if (getRecordRequest().useControlProtocol()) {
			getRecordRequest().processControlMessage(asyncCtx, requestMessage);
		} else {
			getRecordRequest().getHandler().onRecordRequest(getRecordRequest());
		}
	}

	public RecordRequestImpl getRecordRequest() {
		return (RecordRequestImpl) contentRequest;
	}

}
