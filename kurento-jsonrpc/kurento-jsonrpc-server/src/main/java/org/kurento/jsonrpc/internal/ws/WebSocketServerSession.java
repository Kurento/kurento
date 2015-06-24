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
package org.kurento.jsonrpc.internal.ws;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonRpcException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.TransportException;
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.server.ServerSession;
import org.kurento.jsonrpc.internal.server.SessionsManager;
import org.kurento.jsonrpc.message.MessageUtils;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonElement;

public class WebSocketServerSession extends ServerSession {

	private static final long TIMEOUT = PropertiesManager.getProperty(
			"jsonRpcServerWebSocket.timeout", 10000);

	private static Logger log = LoggerFactory
			.getLogger(WebSocketServerSession.class);

	private WebSocketSession wsSession;

	private final PendingRequests pendingRequests = new PendingRequests();

	private ExecutorService execService = Executors.newCachedThreadPool();

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
					Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {
				sendRequestWebSocket(request, resultClass, continuation);
			}
		});
	}

	protected void sendRequestWebSocket(
			final Request<? extends Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		// FIXME: Poor man async implementation.
		execService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					Response<JsonElement> result = sendRequestWebSocket(
							request, resultClass);
					try {
						continuation.onSuccess(result);
					} catch (Exception e) {
						log.error("Exception while processing response", e);
					}
				} catch (Exception e) {
					continuation.onError(e);
				}
			}
		});
	}

	private <P, R> Response<R> sendRequestWebSocket(Request<P> request,
			Class<R> resultClass) {

		log.info("Req-> {}", request.toString());

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
			throw new KurentoException("Exception while sending message '"
					+ JsonUtils.toJson(request)
					+ "' to websocket with native sessionId '"
					+ wsSession.getId() + "'", e);
		}

		if (responseFuture == null) {
			return null;
		}

		Response<JsonElement> responseJsonObject;
		try {
			responseJsonObject = responseFuture.get(TIMEOUT,
					TimeUnit.MILLISECONDS);

			log.info("<-Res {}", responseJsonObject.toString());

		} catch (InterruptedException e) {
			// TODO What to do in this case?
			throw new JsonRpcException(
					"Interrupted while waiting for a response", e);
		} catch (ExecutionException e) {
			// TODO Is there a better way to handle this?
			throw new JsonRpcException("This exception shouldn't be thrown", e);
		} catch (TimeoutException e) {
			throw new TransportException("Timeout of " + TIMEOUT
					+ " milliseconds waiting from response to request with id:"
					+ request.getId(), e);
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
			execService.shutdown();
			wsSession.close();
		} finally {
			super.close();
		}
	}

	public void updateWebSocketSession(WebSocketSession wsSession) {
		synchronized (wsSession) {
			this.wsSession = wsSession;
		}
	}

	@Override
	public void closeNativeSession(String reason) {
		try {
			wsSession.close(new CloseStatus(CloseStatus.NORMAL.getCode(),
					reason));
		} catch (IOException e) {
			log.warn("Exception closing webSocket session", e);
		}
	}

}
