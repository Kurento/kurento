package com.kurento.kmf.content.internal.recorder;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RecorderService;
import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * 
 * Handler servlet for Recorder.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class RecorderHandlerServlet extends AbstractContentHandlerServlet {

	/**
	 * Default serial identifier.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(RecorderHandlerServlet.class);

	/**
	 * Autowired Recorder Handler.
	 */
	@Autowired
	private RecorderHandler recorderHandler;

	/**
	 * Look for {@link RecorderService} annotation in the handler class and
	 * check whether or not it is using redirect strategy.
	 * 
	 * @return Redirect strategy (true|false)
	 */
	@Override
	protected boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException {
		try {
			RecorderService recorderService = Class.forName(handlerClass)
					.getAnnotation(RecorderService.class);
			return recorderService.redirect();
		} catch (ClassNotFoundException e) {
			String message = "Cannot recover class " + handlerClass
					+ " on classpath";
			log.error(message);
			throw new ServletException(message);
		}
	}

	/**
	 * Look for {@link RecorderService} annotation in the handler class and
	 * check whether or not it is using JSON control protocol.
	 * 
	 * @return JSON Control Protocol strategy (true|false)
	 */
	@Override
	protected boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException {
		try {
			RecorderService recorderService = Class.forName(handlerClass)
					.getAnnotation(RecorderService.class);
			return recorderService.useControlProtocol();
		} catch (ClassNotFoundException e) {
			String message = "Cannot recover class " + handlerClass
					+ " on classpath";
			log.error(message);
			throw new ServletException(message);
		}
	}

	/**
	 * Check whether or not Recorder Handler is null.
	 * 
	 * @return Boolean value for whether or not recorder hander is null
	 */
	@Override
	protected boolean isHandlerNull() {
		return recorderHandler == null;
	}

	/**
	 * Handler class name accessor (getter).
	 * 
	 * @return Handler simple name
	 */
	@Override
	protected String getHandlerSimpleClassName() {
		return recorderHandler.getClass().getSimpleName();
	}

	/**
	 * Create a content Request instance (as a Spring Bean).
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param contentId
	 *            Content identifier
	 * @return Content Request
	 */
	@Override
	protected AbstractContentRequest createContentRequest(
			AsyncContext asyncCtx, String contentId) {
		return (RecordRequestImpl) KurentoApplicationContextUtils.getBean(
				"recordRequestImpl", recorderHandler, contentRequestManager,
				asyncCtx, contentId, useRedirectStrategy, useControlProtocol);
	}

	/**
	 * Create asynchronous processor instance (thread).
	 * 
	 * @param contentRequest
	 *            Content Request
	 * @param message
	 *            JSON RPC message
	 * @param asyncCtx
	 *            Asynchronous context
	 * @return Asynchronous processor instance (thread)
	 */
	@Override
	protected RejectableRunnable createAsyncRequestProcessor(
			AbstractContentRequest contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return (AsyncRecorderRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncRecorderRequestProcessor", contentRequest,
						message, asyncCtx);
	}

	/**
	 * Logger accessor (getter).
	 * 
	 * @return Logger
	 */
	@Override
	protected Logger getLogger() {
		return log;
	}

}
