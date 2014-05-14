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
package com.kurento.kmf.jsonrpcconnector.internal.client;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonRpcConnectorException;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSender;

public class ClientSession extends AbstractSession {

	private final JsonRpcRequestSender requestSender;

	public ClientSession(String sessionId, Object registerInfo,
			JsonRpcRequestSender jsonRpcRequestSender) {
		super(sessionId, registerInfo);
		this.requestSender = jsonRpcRequestSender;
	}

	@Override
	public void close() throws IOException {
		if (requestSender instanceof JsonRpcClient) {
			((JsonRpcClient) requestSender).close();
		}
	}

	@Override
	public <R> R sendRequest(String method, Class<R> resultClass)
			throws IOException {
		return requestSender.sendRequest(method, resultClass);
	}

	@Override
	public <R> R sendRequest(String method, Object params, Class<R> resultClass)
			throws IOException {
		return requestSender.sendRequest(method, params, resultClass);
	}

	@Override
	public JsonElement sendRequest(String method) throws IOException {
		return requestSender.sendRequest(method);
	}

	@Override
	public JsonElement sendRequest(String method, Object params)
			throws IOException {
		return requestSender.sendRequest(method, params);
	}

	@Override
	public void sendNotification(String method, Object params)
			throws IOException {
		requestSender.sendNotification(method, params);
	}

	@Override
	public void sendNotification(String method) throws IOException {
		requestSender.sendNotification(method);
	}

	@Override
	public void sendRequest(String method, JsonObject params,
			Continuation<JsonElement> continuation) {
		requestSender.sendRequest(method, params, continuation);
	}

	@Override
	public void sendNotification(String method, Object params,
			Continuation<JsonElement> continuation) throws IOException {
		requestSender.sendNotification(method, params, continuation);
	}

	@Override
	public void setReconnectionTimeout(long millis) {
		throw new JsonRpcConnectorException(
				"Reconnection timeout can't be configured in the client");
	}

}
