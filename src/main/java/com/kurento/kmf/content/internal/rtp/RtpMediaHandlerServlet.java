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
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * 
 * Handler servlet for RTP.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class RtpMediaHandlerServlet extends AbstractContentHandlerServlet {

	/**
	 * Default serial identifier.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(RtpMediaHandlerServlet.class);

	/**
	 * Autowired RTP Handler.
	 */
	@Autowired
	private RtpMediaHandler rtpMediaHandler;

	/**
	 * Always return false since RTP handler does not support redirect strategy.
	 * 
	 * @return Redirect strategy (true|false)
	 */
	@Override
	protected boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException {
		return false;
	}

	/**
	 * Always return false since RTP handler does not support JSON control
	 * protocol.
	 * 
	 * @return JSON Control Protocol strategy (true|false)
	 */
	@Override
	protected boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException {
		return true;
	}

	/**
	 * Check whether or not RTP Handler is null.
	 * 
	 * @return Boolean value for whether or not RTP hander is null
	 */
	@Override
	protected boolean isHandlerNull() {
		return rtpMediaHandler == null;
	}

	/**
	 * Handler class name accessor (getter).
	 * 
	 * @return Handler simple name
	 */
	@Override
	protected String getHandlerSimpleClassName() {
		return rtpMediaHandler.getClass().getSimpleName();
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
		return (RtpMediaRequestImpl) KurentoApplicationContextUtils.getBean(
				"rtpMediaRequestImpl", rtpMediaHandler, contentRequestManager,
				asyncCtx, contentId);
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
		return (AsyncRtpMediaRequestProcessor) KurentoApplicationContextUtils
				.getBean("asyncRtpMediaRequestProcessor", contentRequest,
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
