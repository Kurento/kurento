package com.kurento.kmf.jsonrpcconnector.internal;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcErrorException;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;

public abstract class JsonRpcRequestSenderHelper implements
		JsonRpcRequestSender {

	private static Logger LOG = LoggerFactory
			.getLogger(JsonRpcRequestSenderHelper.class);

	protected AtomicInteger id = new AtomicInteger();
	protected String sessionId;

	public JsonRpcRequestSenderHelper() {
	}

	public JsonRpcRequestSenderHelper(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public <R> R sendRequest(String method, Class<R> resultClass)
			throws IOException {
		return sendRequest(method, null, resultClass);
	}

	public <R> R sendRequest(String method, Object params, Class<R> resultClass)
			throws IOException {

		Request<Object> request = new Request<Object>(id.incrementAndGet(),
				method, params);

		if (JsonRpcConfiguration.INJECT_SESSION_ID) {
			request.setSessionId(sessionId);
		}

		return sendRequest(request, resultClass);
	}

	public <P, R> R sendRequest(Request<P> request, Class<R> resultClass)
			throws JsonRpcErrorException, IOException {

		LOG.info("[Server] Message sent: " + request);

		Response<R> response = internalSendRequest(request, resultClass);

		LOG.info("[Server] Message received: " + response);

		if (response == null) {
			return null;
		}

		if (response.getSessionId() != null) {
			sessionId = response.getSessionId();
		}

		if (response.getError() != null) {
			throw new JsonRpcErrorException(response.getError());
		} else {
			return response.getResult();
		}
	}

	public JsonElement sendRequest(String method) throws IOException {
		return sendRequest(method, JsonElement.class);
	}

	public JsonElement sendRequest(String method, Object params)
			throws IOException {
		return sendRequest(method, params, JsonElement.class);
	}

	@Override
	public void sendNotification(String method) throws IOException {
		sendNotification(method, null);
	}

	@Override
	public void sendNotification(String method, Object params)
			throws IOException {

		Request<Object> request = new Request<Object>(null, method, params);

		if (JsonRpcConfiguration.INJECT_SESSION_ID) {
			request.setSessionId(sessionId);
		}

		sendRequest(request, Void.class);
	}

	protected abstract <P, R> Response<R> internalSendRequest(
			Request<P> request, Class<R> resultClass) throws IOException;

}
