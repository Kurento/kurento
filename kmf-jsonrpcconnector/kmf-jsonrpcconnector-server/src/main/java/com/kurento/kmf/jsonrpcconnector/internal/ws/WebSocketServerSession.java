/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.jsonrpcconnector.internal.ws;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonElement;
import com.kurento.kmf.jsonrpcconnector.JsonRpcConnectorException;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.message.MessageUtils;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.server.ServerSession;
import com.kurento.kmf.jsonrpcconnector.internal.server.SessionsManager;

public class WebSocketServerSession extends ServerSession {

	private static Logger LOG = LoggerFactory
			.getLogger(WebSocketServerSession.class);

	private WebSocketSession wsSession;
	private final PendingRequests pendingRequests = new PendingRequests();

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

			@Override
			protected void internalSendRequest(
					Request<? extends Object> request,
					Class<JsonElement> class1,
					Continuation<Response<JsonElement>> continuation) {
				throw new UnsupportedOperationException(
						"Async client is unavailable");
			}
		});
	}

	private <P, R> Response<R> sendRequestWebSocket(Request<P> request,
			Class<R> resultClass) {

		Future<Response<JsonElement>> responseFuture = null;

		if (request.getId() != null) {
			responseFuture = pendingRequests.prepareResponse(request.getId());
		}

		try {
			synchronized (wsSession) {
				wsSession
						.sendMessage(new TextMessage(JsonUtils.toJson(request)));
			}
		} catch (Exception e) {
			LOG.error(
					"Exception while sending message '{}' to websocket with native sessionId '{}': {}",
					JsonUtils.toJson(request), wsSession.getId(), e);
			// TODO Implement retries if possible
			return null;
		}

		if (responseFuture == null) {
			return null;
		}

		Response<JsonElement> responseJsonObject;
		try {
			responseJsonObject = responseFuture.get();
		} catch (InterruptedException e) {
			// TODO What to do in this case?
			throw new JsonRpcConnectorException(
					"Interrupted while waiting for a response", e);
		} catch (ExecutionException e) {
			// TODO Is there a better way to handle this?
			throw new JsonRpcConnectorException(
					"This exception shouldn't be thrown", e);
		}

		return MessageUtils.convertResponse(responseJsonObject, resultClass);
	}

	@Override
	public void handleResponse(Response<JsonElement> response) {
		pendingRequests.handleResponse(response);
	}

	@Override
	public void close() throws IOException {
		try {
			wsSession.close();
		} finally {
			super.close();
		}
	}

}
