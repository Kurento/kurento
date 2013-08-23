package com.kurento.kmf.content.internal.base;

import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.ERROR_SERVER_ERROR;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

public abstract class AbstractAsyncContentRequestProcessor implements
		RejectableRunnable {

	protected AbstractContentRequest contentRequest;
	protected JsonRpcRequest requestMessage;
	protected AsyncContext asyncCtx;

	public AbstractAsyncContentRequestProcessor(
			AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		this.contentRequest = contentRequest;
		this.requestMessage = requestMessage;
		this.asyncCtx = asyncCtx;
	}

	protected abstract Logger getLogger();

	protected abstract void reallyRun() throws Throwable;

	@Override
	public void run() {
		try {
			reallyRun();
		} catch (Throwable t) {
			getLogger()
					.error("Error processing request to "
							+ ((HttpServletRequest) asyncCtx.getRequest())
									.getRequestURI(),
							t);
			// TODO: different exceptions for server error and request errors
			// (client errors) should be managed
			contentRequest.terminate(true, asyncCtx, ERROR_SERVER_ERROR,
					t.getMessage(), getRequestId());
		}
	}

	private int getRequestId() {
		return requestMessage != null ? requestMessage.getId() : 0;
	}

	@Override
	public void onExecutionRejected() {
		// This reject is executed by an JVM managed thread. We need to specify
		// asyncCtx before terminating
		contentRequest.terminate(true, asyncCtx, ERROR_SERVER_ERROR,
				"Servler overloaded. Try again in a few minutes",
				getRequestId());
	}

}
