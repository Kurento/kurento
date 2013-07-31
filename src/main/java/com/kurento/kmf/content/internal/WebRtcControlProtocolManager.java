package com.kurento.kmf.content.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonResponse;
import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonRequest;

public class WebRtcControlProtocolManager {
	private static final Logger log = LoggerFactory
			.getLogger(WebRtcControlProtocolManager.class);

	private Gson gson;

	public WebRtcControlProtocolManager() {
		gson = new Gson();
	}

	public WebRtcJsonRequest receiveJsonRequest(AsyncContext asyncCtx)
			throws IOException {
		HttpServletRequest request = (HttpServletRequest) asyncCtx.getRequest();
		// TODO: we assume UTF-8 as character encoding, we should try to support
		// others.
		InputStreamReader isr = new InputStreamReader(request.getInputStream(),
				"UTF-8");
		WebRtcJsonRequest jsonRequest = gson.fromJson(isr,
				WebRtcJsonRequest.class);
		Assert.notNull(jsonRequest.getMethod());
		return jsonRequest;

	}

	public void sendJsonAnswer(AsyncContext asyncCtx, WebRtcJsonResponse message)
			throws IOException {
		internalSendJsonAnswer(asyncCtx, message);
	}

	public void sendJsonError(AsyncContext asyncCtx, WebRtcJsonResponse message) {
		try {
			internalSendJsonAnswer(asyncCtx, message);
		} catch (Throwable e) {
			// TODO: perhaps debug
			log.info("Cannot send answer message to destination", e);
		} finally {
			if (asyncCtx != null) {
				asyncCtx.complete();
			}
		}
	}

	private void internalSendJsonAnswer(AsyncContext asyncCtx,
			WebRtcJsonResponse message) throws IOException {
		if (asyncCtx == null) {
			throw new IOException("Cannot recover thread local AsyncContext");
		}

		if (!asyncCtx.getRequest().isAsyncStarted()) {
			return;
		}

		synchronized (asyncCtx) {
			HttpServletResponse response = (HttpServletResponse) asyncCtx
					.getResponse();
			response.setContentType("application/json; charset=UTF-8");
			OutputStreamWriter osw = new OutputStreamWriter(
					response.getOutputStream(), "UTF-8");
			osw.write(gson.toJson(message));
			osw.flush();
			asyncCtx.complete();
		}
	}
}
