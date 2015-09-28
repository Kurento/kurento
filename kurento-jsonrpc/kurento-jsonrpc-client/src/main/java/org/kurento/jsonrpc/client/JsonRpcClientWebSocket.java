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
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.TimeoutReentrantLock;
import org.kurento.commons.TimeoutRuntimeException;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonRpcHandler;
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonRpcClientWebSocket extends JsonRpcClient {

	private static final int MAX_PACKET_SIZE = 1000000;

	private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
			.setNameFormat("JsonRpcClientWebsocket-%d").build();

	@WebSocket
	public class WebSocketClientSocket {

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
				public void sendPingResponse(Message message) throws IOException {
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

	public static Logger log = LoggerFactory.getLogger(JsonRpcClientWebSocket.class);

	public long requestTimeout = PropertiesManager.getProperty("jsonRpcClientWebSocket.timeout", 60000);

	private final CountDownLatch latch = new CountDownLatch(1);

	private volatile ExecutorService execService;
	private volatile ExecutorService disconnectExecService;

	private final String url;
	private volatile Session wsSession;
	private final PendingRequests pendingRequests = new PendingRequests();
	private ResponseSender rs;

	private final SslContextFactory sslContextFactory;
	private final JsonRpcWSConnectionListener connectionListener;

	private WebSocketClient client;

	private boolean reconnecting;

	private TimeoutReentrantLock lock;

	private boolean sendCloseMessage = false;

	private boolean concurrentServerRequest = false;

	public JsonRpcClientWebSocket(String url) {
		this(url, null, null);
	}

	public JsonRpcClientWebSocket(String url, SslContextFactory sslContextFactory) {
		this(url, null, sslContextFactory);
	}

	public JsonRpcClientWebSocket(String url, JsonRpcWSConnectionListener connectionListener) {
		this(url, connectionListener, null);
	}

	public JsonRpcClientWebSocket(String url, JsonRpcWSConnectionListener connectionListener,
			SslContextFactory sslContextFactory) {

		this.lock = new TimeoutReentrantLock(15000, "Server " + url);

		this.sslContextFactory = sslContextFactory;
		this.url = url;
		this.connectionListener = connectionListener;

		rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			protected void internalSendRequest(Request<? extends Object> request, Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {

				internalSendRequestWebSocket(request, resultClass, continuation);
			}

			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request, Class<R> resultClass) throws IOException {

				return internalSendRequestWebSocket(request, resultClass);
			}
		};
	}

	@Override
	public void close() throws IOException {

		super.close();

		String sessionId = this.session != null ? this.session.getSessionId() : "";
		log.info("{} Explicit close of JsonRpcClientWebsocket with sessionId={}", label, sessionId);

		if (sendCloseMessage) {
			try {
				sendRequest(JsonRpcConstants.METHOD_CLOSE);
			} catch (Exception e) {
				log.warn("{} Exception sending close message. {}:{}", label, e.getClass().getName(), e.getMessage());
			}
		}

		if (wsSession != null) {
			wsSession.close();
		} else {
			log.warn("{} Trying to close a JsonRpcClientWebSocket with wsSession=null", label);
		}
		pendingRequests.closeAllPendingRequests();
		this.disableHeartbeat();

		this.closeClient();

	}

	public void setConcurrentServerRequest(boolean concurrentServerRequest) {
		this.concurrentServerRequest = concurrentServerRequest;
	}

	public boolean isConcurrentServerRequest() {
		return concurrentServerRequest;
	}

	@Override
	protected void closeWithReconnection() {
		log.info("{} Closing websocket session to force reconnection", label);
		this.wsSession.close();
		handleReconnectDisconnection(999, "ping timeout");
	}

	public void closeNativeSession() {
		wsSession.close();
	}

	@Override
	public void connect() throws IOException {
		connectIfNecessary();
	}

	public void connectIfNecessary() throws IOException {

		lock.tryLockTimeout("connectIfNecessary()");

		try {

			if (((wsSession == null) || !wsSession.isOpen()) && !isClosed()) {

				log.debug("{} Connecting webSocket client to server {}", label, url);

				try {
					if (client == null) {
						client = new WebSocketClient(sslContextFactory);

						client.setConnectTimeout(this.connectionTimeout);

						WebSocketPolicy policy = client.getPolicy();
						policy.setMaxBinaryMessageBufferSize(MAX_PACKET_SIZE);
						policy.setMaxTextMessageBufferSize(MAX_PACKET_SIZE);
						policy.setMaxBinaryMessageSize(MAX_PACKET_SIZE);
						policy.setMaxTextMessageSize(MAX_PACKET_SIZE);

						client.start();

					} else {
						log.debug("{} Using existing websocket client when session is either null or closed.", label);
					}

					// TODO this should go in the JsonRpcClient
					if (heartbeating) {
						enableHeartbeat();
					}

					// FIXME Give the client some time, otherwise the exception
					// is not thrown if the server is down.
					// Thread.sleep(100);

					WebSocketClientSocket socket = new WebSocketClientSocket();
					ClientUpgradeRequest request = new ClientUpgradeRequest();
					wsSession = client.connect(socket, new URI(url), request).get(this.connectionTimeout,
							TimeUnit.MILLISECONDS);

					wsSession.setIdleTimeout(this.idleTimeout);

				} catch (TimeoutException e) {

					fireConnectionFailed();

					this.closeClient();
					throw new KurentoException(label + " Timeout of " + this.connectionTimeout
							+ "ms when waiting to connect to Websocket server " + url);

				} catch (Exception e) {

					fireConnectionFailed();

					this.closeClient();
					throw new KurentoException(label + " Exception connecting to WebSocket server " + url, e);
				}

				try {

					if (!latch.await(this.connectionTimeout, TimeUnit.MILLISECONDS)) {

						fireConnectionFailed();

						this.closeClient();
						throw new KurentoException(label + " Timeout of " + this.connectionTimeout
								+ "ms when waiting to connect to Websocket server " + url);
					}

					if (session == null) {

						session = new ClientSession(null, null, JsonRpcClientWebSocket.this);

						handlerManager.afterConnectionEstablished(session);

					} else {

						try {
							rsHelper.sendRequest(METHOD_RECONNECT, String.class);

							log.info("{} Reconnected to the same session in server {}", label, url);

							fireReconnectedSameServer();

						} catch (JsonRpcErrorException e) {
							if (e.getCode() == 40007) { // Invalid session
								// exception

								rsHelper.setSessionId(null);
								rsHelper.sendRequest(METHOD_RECONNECT, String.class);

								pendingRequests.closeAllPendingRequests();

								log.info("{} Reconnected to a new session in server {}", label, url);

								fireReconnectedNewServer();

							} else {
								log.warn("{} Error sending reconnection request to server ", label, url, e);
							}
						}
					}

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

		} catch (TimeoutRuntimeException e) {

			log.error(
					"{} Timeout exception trying to acquire lock in JsonRpcWebSocket client to server {}. Closing this client.",
					label, url, e);

			this.closeClient();

		} finally {
			lock.unlock();
		}
	}

	private void fireReconnectedNewServer() {
		if (connectionListener != null) {
			execService.submit(new Runnable() {
				@Override
				public void run() {
					connectionListener.reconnected(false);
				}
			});
		}
	}

	private void fireReconnectedSameServer() {
		if (connectionListener != null) {
			execService.submit(new Runnable() {
				@Override
				public void run() {
					connectionListener.reconnected(true);
				}
			});
		}
	}

	private void fireConnectionFailed() {
		if (connectionListener != null) {
			createExecServiceIfNecessary();
			execService.submit(new Runnable() {
				@Override
				public void run() {
					connectionListener.connectionFailed();
				}
			});
		}
	}

	public Session getWebSocketSession() {
		return wsSession;
	}

	protected void handleReconnectDisconnection(final int statusCode, final String closeReason) {

		if (!isClosed()) {

			reconnecting = true;

			createExecServiceIfNecessary();

			disconnectExecService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						connectIfNecessary();

						reconnecting = false;
					} catch (KurentoException e) {

						log.debug("{} WebSocket closed due to: {}", label, closeReason);

						pendingRequests.closeAllPendingRequests();

						handlerManager.afterConnectionClosed(session, closeReason);

						wsSession = null;

						if (connectionListener != null) {
							connectionListener.disconnected();
						}

					} catch (IOException e) {
						log.warn("{} Exception trying to reconnect to server {}", label, url, e);
					}
				}
			});

		} else {

			pendingRequests.closeAllPendingRequests();

			handlerManager.afterConnectionClosed(session, closeReason);

			if (connectionListener != null) {
				connectionListener.disconnected();
			}
		}
	}

	private void createExecServiceIfNecessary() {

		lock.tryLockTimeout("createExecServiceIfNecessary");

		try {

			if ((execService == null) || execService.isShutdown() || execService.isTerminated()) {
				execService = Executors.newCachedThreadPool(threadFactory);
			}

			if ((disconnectExecService == null) || disconnectExecService.isShutdown()
					|| disconnectExecService.isTerminated()) {
				disconnectExecService = Executors.newCachedThreadPool(threadFactory);
			}
		} finally {
			lock.unlock();
		}
	}

	private void handleRequestFromServer(final JsonObject message) {

		if (concurrentServerRequest) {

			createExecServiceIfNecessary();

			execService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						handlerManager.handleRequest(session, fromJsonRequest(message, JsonElement.class), rs);
					} catch (IOException e) {
						log.warn("{} Exception processing request {}", label, message, e);
					}
				}
			});

		} else {
			try {
				handlerManager.handleRequest(session, fromJsonRequest(message, JsonElement.class), rs);
			} catch (Exception e) {
				log.warn("{} Exception processing request {}", label, message, e);
			}
		}
	}

	private void handleResponseFromServer(JsonObject message) {

		Response<JsonElement> response = fromJsonResponse(message, JsonElement.class);

		setSessionId(response.getSessionId());

		pendingRequests.handleResponse(response);
	}

	private void handleWebSocketTextMessage(String message) {

		try {

			JsonObject jsonMessage = fromJson(message, JsonObject.class);

			if (jsonMessage.has(JsonRpcConstants.METHOD_PROPERTY)) {
				handleRequestFromServer(jsonMessage);
			} else {
				handleResponseFromServer(jsonMessage);
			}

		} catch (Exception e) {
			log.error("{} Exception processing jsonRpc message {}", label, message, e);
		}
	}

	protected <P> void internalSendRequestWebSocket(final Request<P> request, final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		try {

			connectIfNecessary();

			ListenableFuture<Response<JsonElement>> responseFuture = null;

			if (request.getId() != null) {
				responseFuture = pendingRequests.prepareResponse(request.getId());
			}

			final boolean isPing;
			String jsonMessage = request.toString();
			if (METHOD_PING.equals(request.getMethod())) {
				isPing = true;
				log.trace("{} Req-> {}", label, jsonMessage.trim());
			} else {
				isPing = false;
				log.debug("{} Req-> {}", label, jsonMessage.trim());
			}

			if (wsSession == null) {
				// SERVER_ERROR
				throw new IllegalStateException(
						label + " JsonRpcClient is disconnected from WebSocket server at '" + this.url + "'");
			}

			synchronized (wsSession) {
				wsSession.getRemote().sendString(jsonMessage);
			}
			
			createExecServiceIfNecessary();

			if (responseFuture != null) {

				Futures.addCallback(responseFuture, new FutureCallback<Response<JsonElement>>() {
					public void onSuccess(Response<JsonElement> responseJson) {

						if (isPing) {
							log.trace("{} <-Res {}", label, responseJson.toString());
						} else {
							log.debug("{} <-Res {}", label, responseJson.toString());
						}

						try {
							Response<JsonElement> response = MessageUtils.convertResponse(responseJson, resultClass);

							if (response.getSessionId() != null) {
								session.setSessionId(response.getSessionId());
							}

							continuation.onSuccess(response);

						} catch (Exception e) {
							continuation.onError(e);
						}
					}

					public void onFailure(Throwable thrown) {
						continuation.onError(thrown);
					}
				}, execService);

			}

		} catch (Exception e) {
			continuation.onError(e);
		}
	}

	private <P, R> Response<R> internalSendRequestWebSocket(Request<P> request, Class<R> resultClass)
			throws IOException {

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
					label + " JsonRpcClient is disconnected from WebSocket server at '" + this.url + "'");
		}

		synchronized (wsSession) {
			wsSession.getRemote().sendString(jsonMessage);
		}

		if (responseFuture == null) {
			return null;
		}

		Response<JsonElement> responseJson;
		try {
			responseJson = responseFuture.get(requestTimeout, TimeUnit.MILLISECONDS);

			if (isPing) {
				log.trace("{} <-Res {}", label, responseJson.toString());
			} else {
				log.debug("{} <-Res {}", label, responseJson.toString());
			}

			Response<R> response = MessageUtils.convertResponse(responseJson, resultClass);

			if (response.getSessionId() != null) {
				session.setSessionId(response.getSessionId());
			}

			return response;

		} catch (InterruptedException e) {
			// TODO What to do in this case?
			throw new KurentoException(label + " Interrupted while waiting for a response", e);
		} catch (ExecutionException e) {
			// TODO Is there a better way to handle this?
			throw new KurentoException(label + " This exception shouldn't be thrown", e);
		} catch (TimeoutException e) {
			throw new TransportException(label + " Timeout of " + requestTimeout
					+ " milliseconds waiting from response to request " + jsonMessage.trim(), e);
		}
	}

	private void closeClient() {
		if (client != null) {
			log.debug("{} Closing client", label);
			try {
				client.stop();
				client.destroy();
			} catch (Exception e) {
				log.debug("{} Could not properly close websocket client. Reason: {}", label, e.getMessage());
			}
			client = null;
		}

		if (execService != null) {
			try {
				execService.shutdown();
			} catch (Exception e) {
				log.debug("{} Could not properly shut down executor service. Reason: {}", label, e.getMessage());
			}
			execService = null;
		}

		if (disconnectExecService != null) {
			try {
				disconnectExecService.shutdown();
			} catch (Exception e) {
				log.debug("{} Could not properly shut down disconnect executor service. Reason: {}", label,
						e.getMessage());
			}
			disconnectExecService = null;
		}
	}

	@Override
	public void setRequestTimeout(long timeout) {
		this.requestTimeout = timeout;
	}

	public long getRequestTimeout() {
		return requestTimeout;
	}

	public void setSendCloseMessage(boolean sendCloseMessage) {
		this.sendCloseMessage = sendCloseMessage;
	}

	public boolean isSendCloseMessage() {
		return sendCloseMessage;
	}
}
