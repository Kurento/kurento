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
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class RecorderHandlerServlet extends AbstractContentHandlerServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory
			.getLogger(RecorderHandlerServlet.class);

	@Autowired
	private RecorderHandler recorderHandler;

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

	@Override
	protected boolean isHandlerNull() {
		return recorderHandler == null;
	}

	@Override
	protected String getHandlerSimpleClassName() {
		return recorderHandler.getClass().getSimpleName();
	}

	@Override
	protected AbstractContentRequest createContentRequest(
			AsyncContext asyncCtx, String contentId) {
		return (RecordRequestImpl) KurentoApplicationContextUtils.getBean(
				"recordRequestImpl", recorderHandler, contentRequestManager,
				asyncCtx, contentId, useRedirectStrategy, useControlProtocol);
	}

	@Override
	protected RejectableRunnable createAsyncRequestProcessor(
			AbstractContentRequest contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return (AsyncRecorderRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncRecorderRequestProcessor", contentRequest,
						message, asyncCtx);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

}
