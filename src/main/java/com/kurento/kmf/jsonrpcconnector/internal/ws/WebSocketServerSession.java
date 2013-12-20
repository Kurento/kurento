package com.kurento.kmf.jsonrpcconnector.internal.ws;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonElement;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.message.MessageUtils;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.server.ServerSession;
import com.kurento.kmf.jsonrpcconnector.internal.server.SessionsManager;

public class WebSocketServerSession extends ServerSession {

	private WebSocketSession wsSession;
	private PendingRequests pendingRequests = new PendingRequests();

	public WebSocketServerSession(String sessionId, Object registerInfo,
			SessionsManager sessionsManager, WebSocketSession wsSession) {

		super(sessionId, registerInfo, sessionsManager, wsSession.getId());

		this.wsSession = wsSession;

		this.setRsHelper(new JsonRpcRequestSenderHelper(sessionId) {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				return sendRequestWebSocket(request, resultClass);
			}
		});
	}

	private <P, R> Response<R> sendRequestWebSocket(Request<P> request,
			Class<R> resultClass) throws IOException {

		Future<Response<JsonElement>> responseFuture = null;

		if (request.getId() != null) {
			responseFuture = pendingRequests.prepareResponse(request.getId());
		}

		wsSession.sendMessage(new TextMessage(JsonUtils.toJson(request)));

		if (responseFuture == null) {
			return null;
		}

		Response<JsonElement> responseJsonObject;
		try {
			responseJsonObject = responseFuture.get();
		} catch (InterruptedException e) {
			// TODO What to do in this case?
			throw new RuntimeException(
					"Interrupted while waiting for a response", e);
		} catch (ExecutionException e) {
			// TODO Is there a better way to handle this?
			throw new RuntimeException("This exception shouldn't be thrown", e);
		}

		return MessageUtils.convertResponse(responseJsonObject, resultClass);
	}

	public void handleResponse(Response<JsonElement> response) {
		pendingRequests.handleResponse(response);
	}

	public void close() throws IOException {
		try {
			wsSession.close();
		} finally {
			super.close();
		}
	}

}
