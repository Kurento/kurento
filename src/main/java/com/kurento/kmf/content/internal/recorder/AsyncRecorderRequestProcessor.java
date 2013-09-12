package com.kurento.kmf.content.internal.recorder;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractAsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

/**
 * 
 * Asynchronous processor for Recorder.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class AsyncRecorderRequestProcessor extends
		AbstractAsyncContentRequestProcessor {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(AsyncRecorderRequestProcessor.class);

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
	public AsyncRecorderRequestProcessor(AbstractContentRequest contentRequest,
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
	 * Actual implementation for thread execution for a recorder.
	 */
	@Override
	protected void reallyRun() throws Throwable {
		if (getRecordRequest().useControlProtocol()) {
			getRecordRequest().processControlMessage(asyncCtx, requestMessage);
		} else {
			getRecordRequest().getHandler().onRecordRequest(getRecordRequest());
		}
	}

	/**
	 * Record Request accessor (getter).
	 * 
	 * @return Record Request object
	 */
	public RecordRequestImpl getRecordRequest() {
		return (RecordRequestImpl) contentRequest;
	}

}
