package com.kurento.kmf.content.internal;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonRequest;

import static com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonConstants.*;

public class AsyncWebRtcMediaRequestProcessor implements RejectableRunnable {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncWebRtcMediaRequestProcessor.class);

	@Autowired
	private WebRtcControlProtocolManager protocolManager;
	private WebRtcMediaRequestImpl mediaRequest;
	private WebRtcJsonRequest message;
	private AsyncContext asyncCtx;

	public AsyncWebRtcMediaRequestProcessor(
			WebRtcMediaRequestImpl mediaRequest, WebRtcJsonRequest message,
			AsyncContext asyncCtx) {
		this.mediaRequest = mediaRequest;
		this.message = message;
		this.asyncCtx = asyncCtx;
	}

	@Override
	public void run() {
		try {
			mediaRequest.processControlMessage(asyncCtx, message);
		} catch (Throwable t) {
			log.error(
					"Error processing WebRtcControlRequest to "
							+ ((HttpServletRequest) asyncCtx.getRequest())
									.getRequestURI(), t);
			//TODO: different exceptions for server error and request errors (client errors) should be managed
			mediaRequest.terminate(asyncCtx, ERROR_SERVER_ERROR, t.getMessage(), message.getId());
		}
	}

	@Override
	public void onExecutionRejected() {
		// This reject is executed by an JVM managed thread. We need to specify
		// asyncCtx before terminating		
		mediaRequest.terminate(asyncCtx, ERROR_SERVER_ERROR,
				"Servler overloaded. Try again in a few minutes", message.getId());
	}
}
