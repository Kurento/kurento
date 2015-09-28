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
package org.kurento.jsonrpc.internal.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.AbstractSession;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class ServerSession extends AbstractSession {

	private final SessionsManager sessionsManager;
	private JsonRpcRequestSenderHelper rsHelper;
	private String transportId;
	private ScheduledFuture<?> closeTimerTask;
	
	private volatile ConcurrentMap<String, Object> attributes;

	// TODO Make this configurable
	private long reconnectionTimeoutInMillis = 10000;

	public ServerSession(String sessionId, Object registerInfo,
			SessionsManager sessionsManager, String transportId) {

		super(sessionId, registerInfo);

		this.transportId = transportId;
		this.sessionsManager = sessionsManager;
	}

	public abstract void handleResponse(Response<JsonElement> response);

	public String getTransportId() {
		return transportId;
	}

	public void setTransportId(String transportId) {
		this.transportId = transportId;
	}

	@Override
	public void close() throws IOException {
		this.sessionsManager.remove(this.getSessionId());
	}

	protected void setRsHelper(JsonRpcRequestSenderHelper rsHelper) {
		this.rsHelper = rsHelper;
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
	public void sendNotification(String method) throws IOException {
		rsHelper.sendNotification(method);
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
	
	@Override
	public void sendRequestHonorId(Request<JsonObject> request,
			Continuation<Response<JsonElement>> continuation)
					throws IOException {
		rsHelper.sendRequestHonorId(request, continuation);
	}
	
	@Override
	public Response<JsonElement> sendRequestHonorId(Request<JsonObject> request)
			throws IOException {
		return rsHelper.sendRequestHonorId(request);
	}

	public void setCloseTimerTask(ScheduledFuture<?> closeTimerTask) {
		this.closeTimerTask = closeTimerTask;
	}

	public ScheduledFuture<?> getCloseTimerTask() {
		return closeTimerTask;
	}

	@Override
	public void setReconnectionTimeout(long reconnectionTimeoutInMillis) {
		this.reconnectionTimeoutInMillis = reconnectionTimeoutInMillis;
	}

	public long getReconnectionTimeoutInMillis() {
		return reconnectionTimeoutInMillis;
	}

	@Override
	public Map<String, Object> getAttributes() {
		if (attributes == null) {
			synchronized (this) {
				if (attributes == null) {
					attributes = new ConcurrentHashMap<>();
				}
			}
		}

		return attributes;
	}
	
	public abstract void closeNativeSession(String reason);
}
