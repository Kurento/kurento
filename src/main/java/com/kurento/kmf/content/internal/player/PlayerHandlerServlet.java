package com.kurento.kmf.content.internal.player;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentSession;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * 
 * Handler servlet for Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class PlayerHandlerServlet extends AbstractContentHandlerServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory
			.getLogger(PlayerHandlerServlet.class);

	/**
	 * Look for {@link HttpPlayerService} annotation in the handler class and
	 * check whether or not it is using redirect strategy.
	 * 
	 * @return Redirect strategy (true|false)
	 */
	@Override
	protected boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException {
		try {
			HttpPlayerService playerService = Class.forName(handlerClass)
					.getAnnotation(HttpPlayerService.class);
			return playerService.redirect();
		} catch (ClassNotFoundException e) {
			String message = "Cannot recover class " + handlerClass
					+ " on classpath";
			log.error(message);
			throw new ServletException(message);
		}
	}

	/**
	 * Look for {@link HttpPlayerService} annotation in the handler class and
	 * check whether or not it is using JSON control protocol.
	 * 
	 * @return JSON Control Protocol strategy (true|false)
	 */
	@Override
	protected boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException {
		try {
			HttpPlayerService playerService = Class.forName(handlerClass)
					.getAnnotation(HttpPlayerService.class);
			return playerService.useControlProtocol();
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
		return (HttpPlayerSessionImpl) KurentoApplicationContextUtils.getBean(
				"httpPlayerSessionImpl", handler,
				contentSessionManager, asyncCtx, contentId,
				useRedirectStrategy, useControlProtocol);
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
