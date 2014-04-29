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

import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_EXECUTE;
import static com.kurento.kmf.content.jsonrpc.JsonRpcConstants.METHOD_START;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.common.exception.internal.ExceptionUtils;
import com.kurento.kmf.common.exception.internal.ServletUtils;
import com.kurento.kmf.content.ContentHandler;
import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.content.internal.ContentApiExecutorService;
import com.kurento.kmf.content.internal.ContentApiWebApplicationInitializer;
import com.kurento.kmf.content.internal.ContentAsyncListener;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.ControlProtocolManager;
import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * 
 * Abstract class with the definition for Handler Servlets.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class AbstractContentHandlerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Autowired
	private ContentApiExecutorService executor;

	@Autowired
	private ControlProtocolManager protocolManager;

	@Autowired
	protected ContentHandler<? extends ContentSession> handler;

	protected ContentSessionManager contentSessionManager;

	// protected boolean useRedirectStrategy = true;
	protected Class<?> handlerClass;

	protected boolean useControlProtocol = false;

	protected abstract boolean getUseJsonControlProtocol(Class<?> handlerClass)
			throws ServletException;

	protected abstract AbstractContentSession createContentSession(
			AsyncContext asyncCtx, String contentId);

	protected abstract Logger getLogger();

	@Override
	public void init() throws ServletException {
		super.init();

		// Recover application context associated to this servlet in this
		// context
		AnnotationConfigApplicationContext thisServletContext = KurentoApplicationContextUtils
				.getKurentoServletApplicationContext(this.getClass(),
						this.getServletName());

		// If there is not application context associated to this servlet,
		// create one
		if (thisServletContext == null) {
			// Locate the handler class associated to this servlet
			String handlerClassName = this
					.getInitParameter(ContentApiWebApplicationInitializer.HANDLER_CLASS_PARAM_NAME);
			if (handlerClassName == null || handlerClassName.equals("")) {
				String message = "Cannot find handler class associated to handler servlet with name "
						+ this.getServletConfig().getServletName()
						+ " and class " + this.getClass().getName();
				getLogger().error(message);
				throw new ServletException(message);
			}
			// Create application context for this servlet containing the
			// handler
			thisServletContext = KurentoApplicationContextUtils
					.createKurentoHandlerServletApplicationContext(
							this.getClass(), this.getServletName(),
							this.getServletContext(), handlerClassName);

			// useRedirectStrategy = getUseRedirectStrategy(handlerClass);
			try {
				handlerClass = Class.forName(handlerClassName);
			} catch (ClassNotFoundException e) {
				String message = "Cannot recover class " + handlerClass
						+ " on classpath";
				getLogger().error(message);
				throw new ServletException(message);
			}
			useControlProtocol = getUseJsonControlProtocol(handlerClass);
		}

		// Make this servlet to receive beans to resolve the @Autowired present
		// on it
		KurentoApplicationContextUtils
				.processInjectionBasedOnApplicationContext(this,
						thisServletContext);

		if (useControlProtocol) {
			contentSessionManager = (ContentSessionManager) KurentoApplicationContextUtils
					.getBean("contentSessionManager");
		}

	}

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (useControlProtocol) {
			ServletUtils
					.sendHttpError(
							req,
							resp,
							HttpServletResponse.SC_NOT_IMPLEMENTED,
							"Only POST request are supported for this service. You can enable GET requests "
									+ " by setting useControlProtocol to false on the appropriate handler annotation");
			return;
		}

		getLogger().info("GET request received: " + req.getRequestURI());

		if (!req.isAsyncSupported()) {
			// Async context could not be created. It is not necessary to
			// complete it. Just send error message to
			ServletUtils
					.sendHttpError(
							req,
							resp,
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"AsyncContext could not be started. The application should add \"asyncSupported = true\" in all "
									+ this.getClass().getName()
									+ " instances and in all filters in the associated chain");
			return;
		}
		if (handler == null) {
			ServletUtils
					.sendHttpError(
							req,
							resp,
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							handler.getClass().getSimpleName()
									+ " is null. This error means that you "
									+ "need to provide a valid implementation of interface "
									+ handler.getClass().getSimpleName());
			return;
		}

		String contentId = req.getPathInfo();
		if (contentId != null) {
			contentId = contentId.substring(1);
		}

		AsyncContext asyncCtx = req.startAsync();

		// Add listener for managing error conditions
		asyncCtx.addListener(new ContentAsyncListener());

		doRequest4SimpleHttpProtocol(asyncCtx, contentId, resp);

	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		getLogger().info("POST request received: " + req.getRequestURI());

		if (!req.isAsyncSupported()) {
			// Async context could not be created. It is not necessary to
			// complete it. Just send error message to
			ServletUtils
					.sendHttpError(
							req,
							resp,
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"AsyncContext could not be started. The application should add \"asyncSupported = true\" in all "
									+ this.getClass().getName()
									+ " instances and in all filters in the associated chain");
			return;
		}
		if (handler == null) {
			ServletUtils
					.sendHttpError(
							req,
							resp,
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							handler.getClass().getSimpleName()
									+ " is null. This error means that you "
									+ "need to provide a valid implementation of interface "
									+ handler.getClass().getSimpleName());
			return;
		}

		String contentId = req.getPathInfo();
		if (contentId != null) {
			contentId = contentId.substring(1);
		}

		AsyncContext asyncCtx = req.startAsync();

		// Add listener for managing error conditions
		asyncCtx.addListener(new ContentAsyncListener());

		if (useControlProtocol) {
			doRequest4JsonControlProtocol(asyncCtx, contentId, resp);
		} else {
			// TODO: we should check that the content type correspond to
			// the ones we support. We should avoid receiving application/json
			// here and send a coherent error message in that case because this
			// case corresponds to using incorrectly annotations on handlers
			doRequest4SimpleHttpProtocol(asyncCtx, contentId, resp);
		}
	}

	/**
	 * Generic processor of HTTP request when not using JSON control procotol.
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param contentId
	 *            Content unique identifier
	 * @param resp
	 *            HTTP response
	 * @throws ServletException
	 *             Exception in Servlet
	 * @throws IOException
	 *             Input/Ouput Exception
	 */
	private void doRequest4SimpleHttpProtocol(AsyncContext asyncCtx,
			String contentId, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			AbstractContentSession contentRequest = createContentSession(
					asyncCtx, contentId);

			Future<?> future = executor.getExecutor()
					.submit(createAsyncRequestProcessor(contentRequest, null,
							asyncCtx));
			// Store future and request for using it in case of error
			asyncCtx.getRequest().setAttribute(
					ContentAsyncListener.FUTURE_REQUEST_PROCESSOR_ATT_NAME,
					future);
			asyncCtx.getRequest().setAttribute(
					ContentAsyncListener.CONTENT_REQUEST_ATT_NAME,
					contentRequest);
		} catch (KurentoMediaFrameworkException ke) {
			getLogger().error(ke.getMessage(), ke);
			ServletUtils.sendHttpError(
					(HttpServletRequest) asyncCtx.getRequest(), resp,
					ExceptionUtils.getHttpErrorCode(ke.getCode()),
					ke.getMessage());
		}
	}

	/**
	 * Generic processor of HTTP request when using JSON control protocol.
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param contentId
	 *            Content unique identifier
	 * @param resp
	 *            HTTP response
	 * @throws ServletException
	 *             Exception in servlet
	 * @throws IOException
	 *             Input/Ouput Exception
	 */
	private void doRequest4JsonControlProtocol(AsyncContext asyncCtx,
			String contentId, HttpServletResponse resp)
			throws ServletException, IOException {

		JsonRpcRequest message = null;

		try {
			message = protocolManager.receiveJsonRequest(asyncCtx);

			if (message == null) {
				throw new KurentoMediaFrameworkException(
						"Null json message received", 10020);
			}

			AbstractContentSession contentSession = null;
			String sessionId = message.getParams() != null ? message
					.getParams().getSessionId() : null;

			if (sessionId == null && message.getMethod().equals(METHOD_START)) {
				// Session is created by a start request, we need to fill
				// asyncCtx associated to start requests.
				contentSession = createContentSession(asyncCtx, contentId);
				contentSessionManager.put(contentSession);
			} else if (sessionId == null
					&& message.getMethod().equals(METHOD_EXECUTE)) {
				// Session is created by an execute request, the asyncCtx for
				// start requests must be set to null
				contentSession = createContentSession(null, contentId);
				contentSessionManager.put(contentSession);
			} else if (sessionId != null) {
				contentSession = contentSessionManager.get(sessionId);
				if (contentSession == null) {
					throw new KurentoMediaFrameworkException(
							"Cloud not find contentRequest object associated to sessionId "
									+ sessionId, 10021);
				}
			} else {
				throw new KurentoMediaFrameworkException(
						"Cloud not find required sessionId field in request",
						10022);
			}

			Future<?> future = executor.getExecutor().submit(
					createAsyncRequestProcessor(contentSession, message,
							asyncCtx));

			// Store future for using it in ContentAsyncListener in case of
			// error
			asyncCtx.getRequest().setAttribute(
					ContentAsyncListener.FUTURE_REQUEST_PROCESSOR_ATT_NAME,
					future);
			asyncCtx.getRequest().setAttribute(
					ContentAsyncListener.CONTENT_REQUEST_ATT_NAME,
					contentSession);
			asyncCtx.getRequest()
					.setAttribute(
							ContentAsyncListener.CONTROL_PROTOCOL_REQUEST_MESSAGE_ATT_NAME,
							message);
		} catch (KurentoMediaFrameworkException ke) {
			int reqId = message != null ? message.getId() : 0;
			protocolManager.sendJsonError(
					asyncCtx,
					JsonRpcResponse.newError(
							ExceptionUtils.getJsonErrorCode(ke.getCode()),
							ke.getMessage(), reqId));
		} catch (Throwable t) {
			int reqId = message != null ? message.getId() : 0;
			protocolManager.sendJsonError(asyncCtx, JsonRpcResponse.newError(
					ExceptionUtils.getJsonErrorCode(1), t.getMessage(), reqId));
		}
	}

	private RejectableRunnable createAsyncRequestProcessor(
			AbstractContentSession contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return (AsyncContentRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncContentRequestProcessor", contentRequest,
						message, asyncCtx);
	}
}
