package com.kurento.kmf.content.internal.rtp;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;

import com.kurento.kmf.content.internal.RejectableRunnable;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;

public class RtpMediaHandlerServlet extends AbstractContentHandlerServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected boolean getUseRedirectStrategy(String handlerClass)
			throws ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean getUseJsonControlProtocol(String handlerClass)
			throws ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isHandlerNull() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String getHandlerSimpleClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AbstractContentRequest createContentRequest(
			AsyncContext asyncCtx, String contentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected RejectableRunnable createAsyncRequestProcessor(
			AbstractContentRequest contentRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}

}
