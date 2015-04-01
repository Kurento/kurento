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
package org.kurento.jsonrpc.client;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_PING;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.PONG;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.PONG_PAYLOAD;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.KeepAliveManager;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.internal.JsonRpcRequestSender;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class is used to make request to a server using the JSON-RPC protocol
 * with server events. This protocol can be implemented with two transport
 * types: Websockets or http (for request-response) and long-pooling for server
 * events.
 *
 * Request: The request is a JSON with the following fields:
 * <ul>
 * <li>method: Name of the operation to be executed in the server</li>
 * <li>params: Parameters of the operation</li>
 * <li>id: Used if the operation must return a response. This id is used to
 * identify the response if it cannot be identified by means of underlying
 * transport.</li>
 * </ul>
 *
 * Response: The response is a JSON with the following fields:
 * <ul>
 * <li>result: Result of the operation.</li>
 * <li>error: This field is used if operation generates an error.</li>
 * <li>id: request id</li>
 * </ul>
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 */
public abstract class JsonRpcClient implements JsonRpcRequestSender, Closeable {

	public static Logger log = LoggerFactory
			.getLogger(JsonRpcClient.class.getName());

	protected JsonRpcHandlerManager handlerManager = new JsonRpcHandlerManager();
	protected JsonRpcRequestSenderHelper rsHelper;
	protected Object registerInfo;
	protected ClientSession session;
	protected KeepAliveManager keepAliveManager;
	protected String label = "";
	protected int connectionTimeout = 15000;
	protected int idleTimeout = 300000;
	protected int heartbeatInterval = 0;
	private static final int DEFAULT_HEARTBEAT_INTERVAL = 5000;
	protected boolean heartbeating;

	private ScheduledExecutorService scheduler = Executors
			.newSingleThreadScheduledExecutor();

	private Future<?> heartbeat;

	public void setServerRequestHandler(JsonRpcHandler<?> handler) {
		this.handlerManager.setJsonRpcHandler(handler);
	}

	public void setLabel(String label) {
		this.label = "[" + label + "] ";
	}

	@Override
	public <R> R sendRequest(String method, Class<R> resultClass)
			throws IOException {
		return rsHelper.sendRequest(method, resultClass);
	}

	@Override
	public <R> R sendRequest(String method, Object params, Class<R> resultClass)
			throws IOException {
		return rsHelper.sendRequest(method, params, resultClass);
	}

	@Override
	public JsonElement sendRequest(String method) throws IOException {
		return rsHelper.sendRequest(method);
	}

	@Override
	public JsonElement sendRequest(String method, Object params)
			throws IOException {
		return rsHelper.sendRequest(method, params);
	}

	@Override
	public void sendRequest(String method, JsonObject params,
			Continuation<JsonElement> continuation) {
		rsHelper.sendRequest(method, params, continuation);
	}

	@Override
	public void sendNotification(String method) throws IOException {
		rsHelper.sendNotification(method);
	}

	@Override
	public void sendNotification(String method, Object params,
			Continuation<JsonElement> continuation) throws IOException {
		rsHelper.sendNotification(method, params, continuation);
	}

	@Override
	public void sendNotification(String method, Object params)
			throws IOException {
		rsHelper.sendNotification(method, params);
	}

	@Override
	public Response<JsonElement> sendRequest(Request<JsonObject> request)
			throws IOException {
		return rsHelper.sendRequest(request);
	}

	@Override
	public void sendRequest(Request<JsonObject> request,
			Continuation<Response<JsonElement>> continuation)
			throws IOException {
		rsHelper.sendRequest(request, continuation);
	}

	public Session getSession() {
		return session;
	}

	public void setSessionId(String sessionId) {
		this.rsHelper.setSessionId(sessionId);
		this.session.setSessionId(sessionId);
	}

	/**
	 * Gets the connection timeout, in milliseconds, configured in the client.
	 *
	 * @return the timeout in milliseconds
	 */
	public int getConnectionTimeoutValue() {
		return this.connectionTimeout;
	}

	/**
	 * Sets a connection timeout in milliseconds in the client. If after this
	 * timeout, the client could not connect with the server, the
	 * {@link JsonRpcWSConnectionListener#connectionFailed()} method will be
	 * invoked, and a {@link KurentoException} will be thrown.
	 *
	 * @param connectionTimeout
	 *            the timeout in milliseconds
	 */
	public void setConnectionTimeoutValue(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Gets the idle timeout (i.e. the time after which the session is
	 * considered as idle if no messages have been exchanged), in milliseconds,
	 * configured in the client.
	 *
	 * @return the timeout in milliseconds
	 */
	public int getIdleTimeout() {
		return this.idleTimeout;
	}

	/**
	 * Sets an idle timeout in milliseconds in the client. If after the
	 * configured time, no messages have been exchanged between client and
	 * server, connection is reestablished automatically
	 *
	 * @param idleTimeout
	 *            the timeout in milliseconds
	 */
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	/**
	 * Gets the configured heartbeat interval in milliseconds.
	 *
	 * @return the interval
	 */
	public int getHeartbeatInterval() {
		return this.heartbeatInterval;
	}

	/**
	 * Sets the heartbeat interval in milliseconds.
	 *
	 * @param interval
	 *            in milliseconds
	 */
	public void setHeartbeatInterval(int interval) {
		this.heartbeatInterval = interval;
	}

	public void enableHeartbeat() {
		if (this.heartbeatInterval == 0) {
			this.heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
		}
		this.enableHeartbeat(this.heartbeatInterval);
	}

	public void enableHeartbeat(int interval) {
		this.heartbeating = true;
		this.heartbeatInterval = interval;

		if (scheduler.isShutdown()) {
			scheduler = Executors.newSingleThreadScheduledExecutor();
		}

		heartbeat = scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					JsonObject response = sendRequest(METHOD_PING)
							.getAsJsonObject();

					if (!PONG.equals(response.get(PONG_PAYLOAD).getAsString())) {
						closeHeartbeatOnFailure();
					}
				} catch (Exception e) {
					log.warn("{} Error sending heartbeat to server", label);
					closeHeartbeatOnFailure();
				}
			}
		}, 0, heartbeatInterval, MILLISECONDS);

	}

	/**
	 * Cancels the heartbeat task and closes the client
	 */
	private final void closeHeartbeatOnFailure() {
		log.warn(
				"{} Stopping heartbeat and closing client: failure during heartbeat mechanism",
				label);

		heartbeat.cancel(true);
		scheduler.shutdownNow();

		try {
			closeWithReconnection();
		} catch (IOException e) {
			log.warn("{} Exception while lcosing client", label, e);
		}
	}

	public void disableHeartbeat() {
		if (heartbeating) {
			this.heartbeating = false;
			heartbeat.cancel(true);
			scheduler.shutdownNow();
		}
	}

	public KeepAliveManager getKeepAliveManager() {
		return keepAliveManager;
	}

	public void setKeepAliveManager(KeepAliveManager keepAliveManager) {
		this.keepAliveManager = keepAliveManager;
	}

	public abstract void connect() throws IOException;

	@Override
	public abstract void close() throws IOException;

	public void closeWithReconnection() throws IOException {
		this.close();
	}

}
