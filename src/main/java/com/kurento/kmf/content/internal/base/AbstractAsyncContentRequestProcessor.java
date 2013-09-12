package com.kurento.kmf.content.internal.base;

import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.ERROR_SERVER_ERROR;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

/**
 * 
 * Abstract class with the definition of Content Request, JSON request message,
 * and asynchronous context.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class AbstractAsyncContentRequestProcessor implements
		RejectableRunnable {

	/**
	 * Content request.
	 */
	protected AbstractContentRequest contentRequest;

	/**
	 * JSON request message.
	 */
	protected JsonRpcRequest requestMessage;

	/**
	 * Asynchronous context.
	 */
	protected AsyncContext asyncCtx;

	/**
	 * Parameterized constructor.
	 * 
	 * @param contentRequest
	 *            Content request
	 * @param requestMessage
	 *            JSON request message
	 * @param asyncCtx
	 *            Asynchronous context
	 */
	public AbstractAsyncContentRequestProcessor(
			AbstractContentRequest contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		this.contentRequest = contentRequest;
		this.requestMessage = requestMessage;
		this.asyncCtx = asyncCtx;
	}

	/**
	 * Looger accessor.
	 * 
	 * @return logger
	 */
	protected abstract Logger getLogger();

	/**
	 * Actual implementation of the request processor thread run (for a player,
	 * a recorder, and so on).
	 * 
	 * @throws Throwable
	 *             Error/Exception
	 */
	protected abstract void reallyRun() throws Throwable;

	/**
	 * Thread execution method.
	 */
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

	/**
	 * Request message id accessor (getter).
	 * 
	 * @return Request message id
	 */
	private int getRequestId() {
		return requestMessage != null ? requestMessage.getId() : 0;
	}

	/**
	 * Execution reject event method.
	 */
	@Override
	public void onExecutionRejected() {
		// This reject is executed by an JVM managed thread. We need to specify
		// asyncCtx before terminating
		contentRequest.terminate(true, asyncCtx, ERROR_SERVER_ERROR,
				"Servler overloaded. Try again in a few minutes",
				getRequestId());
	}

}
