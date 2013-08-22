package com.kurento.kmf.content.internal.rtp;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class RtpMediaHandlerServlet extends AbstractContentHandlerServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory
			.getLogger(RtpMediaHandlerServlet.class);

	@Autowired
	private RtpMediaHandler rtpMediaHandler;

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
		return rtpMediaHandler == null;
	}

	@Override
	protected String getHandlerSimpleClassName() {
		return rtpMediaHandler.getClass().getSimpleName();
	}

	@Override
	protected AbstractContentRequest createContentRequest(
			AsyncContext asyncCtx, String contentId) {
		return (RtpMediaRequestImpl) KurentoApplicationContextUtils.getBean(
				"rtpMediaRequestImpl", rtpMediaHandler, contentRequestManager,
				asyncCtx, contentId);
	}

	@Override
	protected RejectableRunnable createAsyncRequestProcessor(
			AbstractContentRequest contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return (AsyncRtpMediaRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncRtpMediaRequestProcessor", contentRequest,
						message, asyncCtx);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

}
