package com.kurento.kmf.content.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcResponse;

public class ControlProtocolManager {
	private static final Logger log = LoggerFactory
			.getLogger(ControlProtocolManager.class);

	private static final int BUFF = 4096;

	private static final String UTF8 = "UTF-8";

	private Gson gson;

	public ControlProtocolManager() {
		gson = new Gson();
	}

	public JsonRpcRequest receiveJsonRequest(AsyncContext asyncCtx)
			throws IOException {
		HttpServletRequest request = (HttpServletRequest) asyncCtx.getRequest();

		// Received character encoding should be UTF-8. In order to check this,
		// the method detectJsonEncoding will be used. Before that, the
		// InputStream read from request.getInputStream() should be cloned
		// (using a ByteArrayOutputStream) to be used on detectJsonEncoding and
		// then for reading the JSON message
		InputStream inputStream = request.getInputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[BUFF];
		int len;
		while ((len = inputStream.read(buffer)) > -1) {
			baos.write(buffer, 0, len);
		}
		baos.flush();

		String encoding = detectJsonEncoding(new ByteArrayInputStream(
				baos.toByteArray()));
		log.debug("Detected JSON encoding: " + encoding);
		if (encoding == null || !encoding.equalsIgnoreCase(UTF8)) {
			throw new IOException(
					"Invalid charset encondig in received JSON request");
		}

		InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(
				baos.toByteArray()), UTF8);
		JsonRpcRequest jsonRequest = gson.fromJson(isr, JsonRpcRequest.class);
		Assert.notNull(jsonRequest.getMethod());
		log.info("Received JsonRpc request ...\n " + jsonRequest.toString());
		return jsonRequest;
	}

	private String detectJsonEncoding(InputStream inputStream)
			throws IOException {
		inputStream.mark(4);
		int mask = 0;
		for (int count = 0; count < 4; count++) {
			int r = inputStream.read();
			if (r == -1) {
				break;
			}
			mask = mask << 1;
			mask |= (r == 0) ? 0 : 1;
		}
		inputStream.reset();
		return match(mask);
	}

	private String match(int mask) {
		switch (mask) {
		case 1:
			// UTF-32BE
		case 5:
			// UTF-16BE
		case 8:
			// UTF-32LE
		case 10:
			// UTF-16LE
			return null;
		default:
			return UTF8;
		}
	}

	public void sendJsonAnswer(AsyncContext asyncCtx, JsonRpcResponse message)
			throws IOException {
		internalSendJsonAnswer(asyncCtx, message);
	}

	public void sendJsonError(AsyncContext asyncCtx, JsonRpcResponse message) {
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
			JsonRpcResponse message) throws IOException {
		if (asyncCtx == null) {
			throw new IOException("Cannot recover thread local AsyncContext");
		}

		if (!asyncCtx.getRequest().isAsyncStarted()) {
			return;
		}

		synchronized (asyncCtx) {
			HttpServletResponse response = (HttpServletResponse) asyncCtx
					.getResponse();
			response.setContentType("application/json");
			OutputStreamWriter osw = new OutputStreamWriter(
					response.getOutputStream(), UTF8);
			osw.write(gson.toJson(message));
			osw.flush();
			log.info("Sent JsonRpc answer ...\n" + message);
			asyncCtx.complete();
		}
	}

	public String toString(Object object) {
		return gson.toJson(object);
	}
}
