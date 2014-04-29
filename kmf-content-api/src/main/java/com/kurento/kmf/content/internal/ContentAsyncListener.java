/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.content.internal;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.base.AbstractContentSession;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;

/**
 * 
 * Listener that will be notified for events in an asynchronous operation
 * initiated on a ServletRequest.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public class ContentAsyncListener implements AsyncListener {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(ContentAsyncListener.class);

	/**
	 * Future identifier (used to store future instances in the asynchronous
	 * context).
	 */
	public static final String FUTURE_REQUEST_PROCESSOR_ATT_NAME = "kurento.future.request.att.name";

	/**
	 * Content request identifier (used to store future instances in the
	 * asynchronous context).
	 */
	public static final String CONTENT_REQUEST_ATT_NAME = "kurento.content.request.att.name";

	/**
	 * Control protocol identifier (used to store future instances in the
	 * asynchronous context).
	 */
	public static final String CONTROL_PROTOCOL_REQUEST_MESSAGE_ATT_NAME = "kurento.cjsonrequest.request.att.name";

	/**
	 * Public constructor required by servlet specification.
	 */
	public ContentAsyncListener() {
	}

	/**
	 * On complete event (for asynchronous context) implementation.
	 */
	@Override
	public void onComplete(AsyncEvent ae) {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();
		log.debug("AsyncListener: onComplete on request to: "
				+ request.getRequestURI());
	}

	/**
	 * On timeout event (for asynchronous context) implementation.
	 */
	@Override
	public void onTimeout(AsyncEvent ae) throws IOException {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();

		log.warn("Code 20011. AsyncListener: onTimeout on request to: "
				+ request.getRequestURI());

		internalCompleteAsyncContext(
				ae,
				20011,
				"Request processing timeout. You may tray to re-send your request later. Persistence of this error may be "
						+ "a symptom of a bug on application logic.");
	}

	/**
	 * On error event (for asynchronous context) implementation.
	 */
	@Override
	public void onError(AsyncEvent ae) throws IOException {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();
		log.error("Code 20015. AsyncListener: onError on request to: "
				+ request.getRequestURI(), ae.getThrowable());
		internalCompleteAsyncContext(ae, 20015, "Error processing request");
	}

	/**
	 * Asynchronous context completion method.
	 * 
	 * @param ae
	 *            Event
	 * @param errorCode
	 *            Status code
	 * @param msg
	 *            Message for the completion
	 * @throws IOException
	 *             Error during completion
	 */
	private void internalCompleteAsyncContext(AsyncEvent ae, int errorCode,
			String msg) throws IOException {
		AsyncContext asyncContext = ae.getAsyncContext();

		// If Handler is still executing, we try to cancel the task.
		Future<?> future = (Future<?>) asyncContext.getRequest().getAttribute(
				FUTURE_REQUEST_PROCESSOR_ATT_NAME);
		if (future != null) {
			future.cancel(true);
		}

		AbstractContentSession contentRequest = (AbstractContentSession) asyncContext
				.getRequest().getAttribute(CONTENT_REQUEST_ATT_NAME);
		JsonRpcRequest jsonRequest = (JsonRpcRequest) asyncContext.getRequest()
				.getAttribute(CONTROL_PROTOCOL_REQUEST_MESSAGE_ATT_NAME);
		if (contentRequest != null) {
			contentRequest.internalTerminateWithError(asyncContext, errorCode,
					msg, jsonRequest);
		}
	}

	/**
	 * On start event (for asynchronous context) implementation.
	 */
	@Override
	public void onStartAsync(AsyncEvent ae) {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();
		log.debug("AsyncListener: onStartAsync on request to: "
				+ request.getRequestURI());
	}

}
