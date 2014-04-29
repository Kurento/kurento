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
package com.kurento.kmf.content.internal.base;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
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
public class AsyncContentRequestProcessor implements RejectableRunnable {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncContentRequestProcessor.class);

	private AbstractContentSession contentSession;

	protected JsonRpcRequest requestMessage;

	protected AsyncContext asyncCtx;

	public AsyncContentRequestProcessor(AbstractContentSession contentRequest,
			JsonRpcRequest requestMessage, AsyncContext asyncCtx) {
		this.contentSession = contentRequest;
		this.requestMessage = requestMessage;
		this.asyncCtx = asyncCtx;
	}

	/**
	 * Thread execution method.
	 */
	@Override
	public void run() {
		try {
			if (contentSession.useControlProtocol()) {
				contentSession.processControlMessage(asyncCtx, requestMessage);
			} else {
				contentSession.callOnContentRequestOnHandler();
			}
		} catch (KurentoMediaFrameworkException kge) {
			log.error(
					"Error processing request to "
							+ ((HttpServletRequest) asyncCtx.getRequest())
									.getRequestURI(), kge);
			contentSession.internalTerminateWithError(asyncCtx, kge.getCode(),
					kge.getMessage(), requestMessage);
		} catch (Throwable t) {
			log.error(
					"Error processing request to "
							+ ((HttpServletRequest) asyncCtx.getRequest())
									.getRequestURI(), t);
			contentSession.internalTerminateWithError(asyncCtx, 1,
					t.getMessage(), requestMessage);
		}
	}

	/**
	 * Execution reject event method.
	 */
	@Override
	public void onExecutionRejected() {
		// This reject is executed by an JVM managed thread. We need to specify
		// asyncCtx before terminating
		contentSession.internalTerminateWithError(asyncCtx, 20011,
				"Servler overloaded. Try again in a few minutes",
				requestMessage);
	}
}
