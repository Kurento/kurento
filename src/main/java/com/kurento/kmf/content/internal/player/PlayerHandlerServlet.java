package com.kurento.kmf.content.internal.player;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * 
 * Handler servlet for Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class PlayerHandlerServlet extends AbstractContentHandlerServlet {

	/**
	 * Default serial identifier.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(PlayerHandlerServlet.class);

	/**
	 * Autowired Player Handler.
	 */
	@Autowired
	private PlayerHandler playerHandler;

	/**
	 * Look for {@link PlayerService} annotation in the handler class and check
	 * whether or not it is using redirect strategy.
	 * 
	 * @return Redirect strategy (true|false)
	 */
	@Override
	protected boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException {
		try {
			PlayerService playerService = Class.forName(handlerClass)
					.getAnnotation(PlayerService.class);
			return playerService.redirect();
		} catch (ClassNotFoundException e) {
			String message = "Cannot recover class " + handlerClass
					+ " on classpath";
			log.error(message);
			throw new ServletException(message);
		}
	}

	/**
	 * Look for {@link PlayerService} annotation in the handler class and check
	 * whether or not it is using JSON control protocol.
	 * 
	 * @return JSON Control Protocol strategy (true|false)
	 */
	@Override
	protected boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException {
		try {
			PlayerService playerService = Class.forName(handlerClass)
					.getAnnotation(PlayerService.class);
			return playerService.useControlProtocol();
		} catch (ClassNotFoundException e) {
			String message = "Cannot recover class " + handlerClass
					+ " on classpath";
			log.error(message);
			throw new ServletException(message);
		}
	}

	/**
	 * Check whether or not Player Handler is null.
	 * 
	 * @return Boolean value for whether or not player hander is null
	 */
	@Override
	protected boolean isHandlerNull() {
		return playerHandler == null;
	}

	/**
	 * Handler class name accessor (get).
	 * 
	 * @return Handler simple name
	 */
	@Override
	protected String getHandlerSimpleClassName() {
		return playerHandler.getClass().getSimpleName();
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
		return (PlayRequestImpl) KurentoApplicationContextUtils.getBean(
				"playRequestImpl", playerHandler, contentRequestManager,
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
		return (AsyncPlayerRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncPlayerRequestProcessor", contentRequest,
						message, asyncCtx);
	}
}
