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

public class PlayerHandlerServlet extends AbstractContentHandlerServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory
			.getLogger(PlayerHandlerServlet.class);

	@Autowired
	private PlayerHandler playerHandler;

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

	@Override
	protected boolean isHandlerNull() {
		return playerHandler == null;
	}

	@Override
	protected String getHandlerSimpleClassName() {
		return playerHandler.getClass().getSimpleName();
	}

	@Override
	protected AbstractContentRequest createContentRequest(
			AsyncContext asyncCtx, String contentId) {
		return (PlayRequestImpl) KurentoApplicationContextUtils.getBean(
				"playRequestImpl", playerHandler, contentRequestManager,
				asyncCtx, contentId, useRedirectStrategy, useControlProtocol);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected RejectableRunnable createAsyncRequestProcessor(
			AbstractContentRequest contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return (AsyncPlayerRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncPlayerRequestProcessor", contentRequest,
						message, asyncCtx);
	}
}
