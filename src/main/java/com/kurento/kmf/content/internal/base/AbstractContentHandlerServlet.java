package com.kurento.kmf.content.internal.base;

import static com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants.ERROR_INTERNAL_ERROR;
import static com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants.ERROR_INVALID_REQUEST;
import static com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants.ERROR_PARSE_ERROR;
import static com.kurento.kmf.content.internal.jsonrpc.JsonRpcConstants.METHOD_START;

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

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.kurento.kmf.content.internal.ContentApiExecutorService;
import com.kurento.kmf.content.internal.ContentApiWebApplicationInitializer;
import com.kurento.kmf.content.internal.ContentAsyncListener;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.ControlProtocolManager;
import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public abstract class AbstractContentHandlerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Autowired
	private ContentApiExecutorService executor;

	@Autowired
	private ControlProtocolManager protocolManager;

	protected ContentRequestManager contentRequestManager;

	protected boolean useRedirectStrategy = true;
	protected boolean useControlProtocol = false;

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
			String handlerClass = this
					.getInitParameter(ContentApiWebApplicationInitializer.HANDLER_CLASS_PARAM_NAME);
			if (handlerClass == null || handlerClass.equals("")) {
				String message = "Cannot find handler class associated to handler servlet with name "
						+ this.getServletConfig().getServletName()
						+ " and class " + this.getClass().getName();
				getLogger().error(message);
				throw new ServletException(message);
			}
			// Create application context for this servlet containing the
			// handler
			thisServletContext = KurentoApplicationContextUtils
					.createKurentoServletApplicationContext(this.getClass(),
							this.getServletName(), this.getServletContext(),
							handlerClass);

			useRedirectStrategy = getUseRedirectStrategy(handlerClass);
			useControlProtocol = getUseJsonControlProtocol(handlerClass);

		}

		// Make this servlet to receive beans to resolve the @Autowired present
		// on it
		KurentoApplicationContextUtils
				.processInjectionBasedOnApplicationContext(this,
						thisServletContext);

		if (useControlProtocol) {
			contentRequestManager = (ContentRequestManager) KurentoApplicationContextUtils
					.getBean("contentRequestManager");
		}

	}

	protected abstract boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException;

	protected abstract boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException;

	protected abstract boolean isHandlerNull();

	protected abstract String getHandlerSimpleClassName();

	protected abstract AbstractContentRequest createContentRequest(
			AsyncContext asyncCtx, String contentId);

	protected abstract RejectableRunnable createAsyncRequestProcessor(
			AbstractContentRequest contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx);

	protected abstract Logger getLogger();

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (useControlProtocol) {
			resp.sendError(
					HttpServletResponse.SC_NOT_IMPLEMENTED,
					"Only POST request are supported for this service. You can enable GET requests "
							+ " by setting useControlProtocol to false on the appropriate handler annotation");
			return;
		}

		getLogger().debug("GET request received: " + req.getRequestURI());

		if (!req.isAsyncSupported()) {
			// Async context could not be created. It is not necessary to
			// complete it. Just send error message to
			resp.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"AsyncContext could not be started. The application should add \"asyncSupported = true\" in all "
							+ this.getClass().getName()
							+ " instances and in all filters in the associated chain");
			return;
		}
		if (isHandlerNull()) {
			resp.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					getHandlerSimpleClassName()
							+ " is null. This error means that you "
							+ "need to provide a valid implementation of interface "
							+ getHandlerSimpleClassName());
			return;
		}

		String contentId = req.getPathInfo();
		if (contentId != null) {
			contentId = contentId.substring(1);
		}

		AsyncContext asyncCtx = req.startAsync();

		// Add listener for managing error conditions
		asyncCtx.addListener(new ContentAsyncListener());

		AbstractContentRequest contentRequest = createContentRequest(asyncCtx,
				contentId);

		Future<?> future = executor.getExecutor().submit(
				createAsyncRequestProcessor(contentRequest, null, asyncCtx));
		// Store future and request for using it in case of error
		req.setAttribute(
				ContentAsyncListener.FUTURE_REQUEST_PROCESSOR_ATT_NAME, future);
		req.setAttribute(ContentAsyncListener.CONTENT_REQUEST_ATT_NAME,
				contentRequest);
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (!useControlProtocol) {
			resp.sendError(
					HttpServletResponse.SC_NOT_IMPLEMENTED,
					"Only GET request are supported for this service. You can enable POST requests "
							+ "by setting useControlProtocol to true on the appropriate handler annotation");
			return;
		}

		getLogger().debug("POST request received: " + req.getRequestURI());

		if (!req.isAsyncSupported()) {
			// Async context could not be created. It is not necessary to
			// complete it. Just send error message to
			resp.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"AsyncContext could not be started. The application should add \"asyncSupported = true\" in all "
							+ this.getClass().getName()
							+ " instances and in all filters in the associated chain");
			return;
		}
		if (isHandlerNull()) {
			resp.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					getHandlerSimpleClassName()
							+ " is null. This error means that you "
							+ "need to provide a valid implementation of interface "
							+ getHandlerSimpleClassName());
			return;
		}

		String contentId = req.getPathInfo();
		if (contentId != null) {
			contentId = contentId.substring(1);
		}

		AsyncContext asyncCtx = req.startAsync();

		// Add listener for managing error conditions
		asyncCtx.addListener(new ContentAsyncListener());

		JsonRpcRequest message = null;
		try {
			message = protocolManager.receiveJsonRequest(asyncCtx);
		} catch (JsonSyntaxException jse) {
			protocolManager.sendJsonError(asyncCtx, JsonRpcResponse.newError(
					ERROR_PARSE_ERROR, "Json syntax is not valid. Reason: "
							+ jse.getMessage(), 0));
			return;
		} catch (JsonIOException jie) {
			protocolManager.sendJsonError(asyncCtx, JsonRpcResponse.newError(
					ERROR_INTERNAL_ERROR,
					"Cloud not read Json string. Reason: " + jie.getMessage(),
					0));
			return;
		}

		if (message == null) {
			protocolManager.sendJsonError(asyncCtx, JsonRpcResponse.newError(
					ERROR_INTERNAL_ERROR,
					"Cannot process message with null action field", 0));
			return;
		}

		AbstractContentRequest contentRequest = null;
		if (message.getMethod().equals(METHOD_START)) {
			contentRequest = createContentRequest(asyncCtx, contentId);
			contentRequestManager.put(contentRequest);
		} else if (message.getSessionId() != null) {
			contentRequest = contentRequestManager.get(message.getSessionId());
			if (contentRequest == null) {
				protocolManager.sendJsonError(asyncCtx, JsonRpcResponse
						.newError(ERROR_INVALID_REQUEST,
								"Cloud not find WebRtcMedia state associated to sessionId "
										+ message.getSessionId(),
								message.getId()));
				return;
			}
		} else {
			protocolManager.sendJsonError(asyncCtx, JsonRpcResponse.newError(
					ERROR_INVALID_REQUEST,
					"Cloud not find required sessionId field in request",
					message.getId()));
			return;
		}

		Future<?> future = executor.getExecutor().submit(
				createAsyncRequestProcessor(contentRequest, message, asyncCtx));

		// Store future for using it in case of error
		req.setAttribute(
				ContentAsyncListener.FUTURE_REQUEST_PROCESSOR_ATT_NAME, future);
		req.setAttribute(ContentAsyncListener.CONTENT_REQUEST_ATT_NAME,
				contentRequest);
		req.setAttribute(
				ContentAsyncListener.CONTROL_PROTOCOL_REQUEST_MESSAGE_ATT_NAME,
				message);
	}
}