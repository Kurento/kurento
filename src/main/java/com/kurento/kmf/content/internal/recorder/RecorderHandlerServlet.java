package com.kurento.kmf.content.internal.recorder;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.HttpRecorderHandler;
import com.kurento.kmf.content.HttpRecorderService;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentSession;
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
	private HttpRecorderHandler recorderHandler;

	/**
	 * Look for {@link HttpRecorderService} annotation in the handler class and
	 * check whether or not it is using redirect strategy.
	 * 
	 * @return Redirect strategy (true|false)
	 */
	@Override
	protected boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException {
		try {
			HttpRecorderService recorderService = Class.forName(handlerClass)
					.getAnnotation(HttpRecorderService.class);
			return recorderService.redirect();
		} catch (ClassNotFoundException e) {
			String message = "Cannot recover class " + handlerClass
					+ " on classpath";
			log.error(message);
			throw new ServletException(message);
		}
	}

	/**
	 * Look for {@link HttpRecorderService} annotation in the handler class and
	 * check whether or not it is using JSON control protocol.
	 * 
	 * @return JSON Control Protocol strategy (true|false)
	 */
	@Override
	protected boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException {
		try {
			HttpRecorderService recorderService = Class.forName(handlerClass)
					.getAnnotation(HttpRecorderService.class);
			return recorderService.useControlProtocol();
		} catch (ClassNotFoundException e) {
			String message = "Cannot recover class " + handlerClass
					+ " on classpath";
			log.error(message);
			throw new ServletException(message);
		}
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
	protected AbstractContentSession createContentSession(
			AsyncContext asyncCtx, String contentId) {
		return (HttpRecorderSessionImpl) KurentoApplicationContextUtils.getBean(
				"recordRequestImpl", recorderHandler, contentSessionManager,
				asyncCtx, contentId, useRedirectStrategy, useControlProtocol);
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
