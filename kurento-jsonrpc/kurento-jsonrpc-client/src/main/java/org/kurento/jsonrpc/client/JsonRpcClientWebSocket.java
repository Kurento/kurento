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
 */
package org.kurento.jsonrpc.client;

import static org.kurento.jsonrpc.JsonUtils.fromJson;
import static org.kurento.jsonrpc.JsonUtils.fromJsonRequest;
import static org.kurento.jsonrpc.JsonUtils.fromJsonResponse;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_PING;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_RECONNECT;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.TransportException;
import org.kurento.jsonrpc.internal.JsonRpcConstants;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.internal.ws.PendingRequests;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.MessageUtils;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonRpcClientWebSocket extends JsonRpcClient {

	private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
			.setNameFormat("JsonRpcClientWebsocket-%d").build();

	@WebSocket(maxTextMessageSize = 64 * 1024)
	public class SimpleEchoSocket {

		@OnWebSocketClose
		public void onClose(int statusCode, String closeReason) {
			handleReconnectDisconnection(statusCode, closeReason);
		}

		@OnWebSocketConnect
		public void onConnect(Session session) {
			wsSession = session;
			rs = new ResponseSender() {
				@Override
				public void sendResponse(Message message) throws IOException {
					String jsonMessage = message.toString();
					log.debug("{} <-Res {}", label, jsonMessage);
					synchronized (wsSession) {
						wsSession.getRemote().sendString(jsonMessage);
					}
				}

				@Override
				public void sendPingResponse(Message message)
						throws IOException {
					String jsonMessage = message.toString();
					log.trace("{} <-Res {}", label, jsonMessage);
					synchronized (wsSession) {
						wsSession.getRemote().sendString(jsonMessage);
					}
				}
			};
			latch.countDown();
			if ((connectionListener != null) && !reconnecting) {
				connectionListener.connected();
			}
		}

		@OnWebSocketMessage
		public void onMessage(String message) {
			handleWebSocketTextMessage(message);
		}

	}

	public static Logger log = LoggerFactory
			.getLogger(JsonRpcClientWebSocket.class);

	public static final long TIMEOUT = 60000;

	private final CountDownLatch latch = new CountDownLatch(1);

	private ExecutorService execService = Executors.newFixedThreadPool(10,
			threadFactory);

	private final String url;
	private volatile Session wsSession;
	private final PendingRequests pendingRequests = new PendingRequests();
	private ResponseSender rs;

	private final SslContextFactory sslContextFactory;
	private final JsonRpcWSConnectionListener connectionListener;

	private boolean clientClose;

	private WebSocketClient client;

	private boolean reconnecting;

	public JsonRpcClientWebSocket(String url) {
		this(url, null, null);
	}

	public JsonRpcClientWebSocket(String url,
			SslContextFactory sslContextFactory) {
		this(url, null, sslContextFactory);
	}

	public JsonRpcClientWebSocket(String url,
			JsonRpcWSConnectionListener connectionListener) {
		this(url, connectionListener, null);
	}

	public JsonRpcClientWebSocket(String url,
			JsonRpcWSConnectionListener connectionListener,
			SslContextFactory sslContextFactory) {
		this.sslContextFactory = sslContextFactory;
		this.url = url;
		this.connectionListener = connectionListener;

		rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			protected void internalSendRequest(
					Request<? extends Object> request,
					Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {

				internalSendRequestWebSocket(request, resultClass, continuation);
			}

			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {

				return internalSendRequestWebSocket(request, resultClass);
			}
		};
	}

	@Override
	public void close() throws IOException {

		log.info("{} Closing JsonRpcClientWebsocket", label);
		if (wsSession != null) {
			wsSession.close();
		}
		this.disableHeartbeat();
		clientClose = true;
		this.closeClient();
	}

	@Override
	public void closeWithReconnection() {
		log.info("{} Closing session with reconnection", label);
		this.wsSession.close();
		this.closeClient();
	}

	public void closeNativeSession() {
		wsSession.close();
	}

	@Override
	public void connect() throws IOException {
		connectIfNecessary();
	}

	public synchronized void connectIfNecessary() throws IOException {

		if (((wsSession == null) || !wsSession.isOpen()) && !clientClose) {

			try {
				if (client == null) {
					client = new WebSocketClient(sslContextFactory);
					client.setConnectTimeout(this.connectionTimeout);
					client.start();
				} else {
					log.debug(
							"{} Using existing websocket client when session is either null or closed.",
							label);
				}

				// TODO this should go in the JsonRpcClient
				if (heartbeating) {
					enableHeartbeat();
				}

				// FIXME Give the client some time, otherwise the exception
				// is not thrown if the server is down.
				Thread.sleep(100);

				SimpleEchoSocket socket = new SimpleEchoSocket();
				ClientUpgradeRequest request = new ClientUpgradeRequest();
				wsSession = client.connect(socket, new URI(url), request).get(
						this.connectionTimeout, TimeUnit.MILLISECONDS);
				wsSession.setIdleTimeout(this.idleTimeout);

			} catch (TimeoutException e) {
				if (connectionListener != null) {
					connectionListener.connectionFailed();
				}

				this.closeClient();
				throw new KurentoException(label + " Timeout of "
						+ this.connectionTimeout
						+ "ms when waiting to connect to Websocket server "
						+ url);

			} catch (Exception e) {
				if (connectionListener != null) {
					connectionListener.connectionFailed();
				}

				this.closeClient();
				throw new KurentoException(label
						+ " Exception connecting to WebSocket server " + url, e);
			}

			try {
				// FIXME: Make this configurable
				if (!latch.await(this.connectionTimeout, TimeUnit.MILLISECONDS)) {
					if (connectionListener != null) {
						connectionListener.connectionFailed();
					}
					this.closeClient();
					throw new KurentoException(label + " Timeout of "
							+ this.connectionTimeout
							+ "ms when waiting to connect to Websocket server "
							+ url);
				}

				if (session == null) {

					session = new ClientSession(null, null,
							JsonRpcClientWebSocket.this);
					handlerManager.afterConnectionEstablished(session);

				} else {

					try {
						rsHelper.sendRequest(METHOD_RECONNECT, String.class);

						log.info(
								"{} Reconnected to the same session in server {}",
								label, url);

					} catch (JsonRpcErrorException e) {
						if (e.getCode() == 40007) { // Invalid session exception

							rsHelper.setSessionId(null);
							rsHelper.sendRequest(METHOD_RECONNECT, String.class);

							log.info(
									"{} Reconnected to a new session in server {}",
									label, url);
						} else {
							log.warn(
									"{} Error sending reconnection request to server ",
									label, url, e);
						}
					}
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public Session getWebSocketSession() {
		return wsSession;
	}

	protected void handleReconnectDisconnection(final int statusCode,
			final String closeReason) {

		if (!clientClose) {

			reconnecting = true;

			if ((execService == null) || execService.isShutdown()
					|| execService.isTerminated()) {
				execService = Executors.newFixedThreadPool(10, threadFactory);
			}

			execService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						connectIfNecessary();

						reconnecting = false;
					} catch (KurentoException e) {

						handlerManager.afterConnectionClosed(session,
								closeReason);

						log.debug("{} WebSocket closed due to: {}", label,
								closeReason);
						wsSession = null;

						if (connectionListener != null) {
							connectionListener.disconnected();
						}

					} catch (IOException e) {
						log.warn(
								"{} Exception trying to reconnect to server {}",
								label, url, e);
					}
				}
			});

		} else {

			handlerManager.afterConnectionClosed(session, closeReason);

			if (connectionListener != null) {
				connectionListener.disconnected();
			}
		}
	}

	private void handleRequestFromServer(final JsonObject message) {

		// TODO: Think better ways to do this:
		// handleWebSocketTextMessage seems to be sequential. That is, the
		// message waits to be processed until previous message is being
		// processed. This behavior doesn't allow making a new request in the
		// handler of an event. To avoid this problem, we have decided to
		// process requests from server in a new thread (reused from
		// ExecutorService).
		execService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					handlerManager.handleRequest(session,
							fromJsonRequest(message, JsonElement.class), rs);
				} catch (IOException e) {
					log.warn("{} Exception processing request {}", label,
							message, e);
				}
			}
		});
	}

	private void handleResponseFromServer(JsonObject message) {

		Response<JsonElement> response = fromJsonResponse(message,
				JsonElement.class);

		setSessionId(response.getSessionId());

		pendingRequests.handleResponse(response);
	}

	private void handleWebSocketTextMessage(String message) {

		JsonObject jsonMessage = fromJson(message, JsonObject.class);

		if (jsonMessage.has(JsonRpcConstants.METHOD_PROPERTY)) {
			handleRequestFromServer(jsonMessage);
		} else {
			handleResponseFromServer(jsonMessage);
		}
	}

	protected void internalSendRequestWebSocket(
			final Request<? extends Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		// FIXME: Poor man async implementation.
		execService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					Response<JsonElement> result = internalSendRequestWebSocket(
							request, resultClass);
					try {
						continuation.onSuccess(result);
					} catch (Exception e) {
						log.error("{} Exception while processing response",
								label, e);
					}
				} catch (Exception e) {
					continuation.onError(e);
				}
			}
		});
	}

	private <P, R> Response<R> internalSendRequestWebSocket(Request<P> request,
			Class<R> resultClass) throws IOException {

		if (request.getMethod().equals("pull")) {
			log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		}

		connectIfNecessary();

		Future<Response<JsonElement>> responseFuture = null;

		if (request.getId() != null) {
			responseFuture = pendingRequests.prepareResponse(request.getId());
		}

		boolean isPing = false;
		String jsonMessage = request.toString();
		if (METHOD_PING.equals(request.getMethod())) {
			isPing = true;
			log.trace("{} Req-> {}", label, jsonMessage.trim());
		} else {
			log.debug("{} Req-> {}", label, jsonMessage.trim());
		}

		if (wsSession == null) {
			// SERVER_ERROR
			throw new IllegalStateException(
					"JsonRpcClient is disconnected from WebSocket server at '"
							+ this.url + "'");
		}

		synchronized (wsSession) {
			wsSession.getRemote().sendString(jsonMessage);
		}

		if (responseFuture == null) {
			return null;
		}

		Response<JsonElement> responseJson;
		try {
			responseJson = responseFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);

			if (isPing) {
				log.trace("{} <-Res {}", label, responseJson.toString());
			} else {
				log.debug("{} <-Res {}", label, responseJson.toString());
			}

			Response<R> response = MessageUtils.convertResponse(responseJson,
					resultClass);

			if (response.getSessionId() != null) {
				session.setSessionId(response.getSessionId());
			}

			return response;

		} catch (InterruptedException e) {
			// TODO What to do in this case?
			throw new KurentoException(label
					+ " Interrupted while waiting for a response", e);
		} catch (ExecutionException e) {
			// TODO Is there a better way to handle this?
			throw new KurentoException(label
					+ " This exception shouldn't be thrown", e);
		} catch (TimeoutException e) {
			throw new TransportException(label + " Timeout of " + TIMEOUT
					+ " milliseconds waiting from response to request with id:"
					+ request.getId(), e);
		}
	}

	private void closeClient() {
		if (client != null) {
			log.debug("{} Stopping client", label);
			try {
				client.stop();
				client.destroy();
			} catch (Exception e) {
				log.debug(
						"{} Could not properly close websocket client. Reason: {}",
						label, e.getMessage());
			}
			client = null;
		}

		if (execService != null) {
			try {
				execService.shutdown();
			} catch (Exception e) {
				log.debug(
						"{} Could not properly shut down executor service. Reason: {}",
						label, e.getMessage());
			}
			execService = null;
		}
	}
}
