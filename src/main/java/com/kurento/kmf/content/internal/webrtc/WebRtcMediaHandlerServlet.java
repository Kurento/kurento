package com.kurento.kmf.content.internal.webrtc;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class WebRtcMediaHandlerServlet extends AbstractContentHandlerServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory
			.getLogger(WebRtcMediaHandlerServlet.class);

	@Autowired
	private WebRtcMediaHandler webRtcMediaHandler;

	@Override
	protected boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException {
		return false;
	}

	@Override
	protected boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException {
		return true;
	}

	@Override
	protected boolean isHandlerNull() {
		return webRtcMediaHandler == null;
	}

	@Override
	protected String getHandlerSimpleClassName() {
		return webRtcMediaHandler.getClass().getSimpleName();
	}

	@Override
	protected AbstractContentRequest createContentRequest(
			AsyncContext asyncCtx, String contentId) {
		return (WebRtcMediaRequestImpl) KurentoApplicationContextUtils.getBean(
				"webRtcMediaRequestImpl", webRtcMediaHandler,
				contentRequestManager, asyncCtx, contentId);
	}

	@Override
	protected RejectableRunnable createAsyncRequestProcessor(
			AbstractContentRequest contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return (AsyncWebRtcMediaRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncWebRtcMediaRequestProcessor", contentRequest,
						message, asyncCtx);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

}
