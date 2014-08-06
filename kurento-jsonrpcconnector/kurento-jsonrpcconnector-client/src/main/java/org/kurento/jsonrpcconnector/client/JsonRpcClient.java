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
package org.kurento.jsonrpcconnector.client;

import java.io.Closeable;
import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.kurento.jsonrpcconnector.JsonRpcHandler;
import org.kurento.jsonrpcconnector.KeepAliveManager;
import org.kurento.jsonrpcconnector.Session;
import org.kurento.jsonrpcconnector.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpcconnector.internal.JsonRpcRequestSender;
import org.kurento.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpcconnector.internal.client.ClientSession;
import org.kurento.jsonrpcconnector.internal.message.Request;
import org.kurento.jsonrpcconnector.internal.message.Response;

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

	protected JsonRpcHandlerManager handlerManager = new JsonRpcHandlerManager();
	protected JsonRpcRequestSenderHelper rsHelper;
	protected Object registerInfo;
	protected ClientSession session;
	protected KeepAliveManager keepAliveManager;

	public void setServerRequestHandler(JsonRpcHandler<?> handler) {
		this.handlerManager.setJsonRpcHandler(handler);
	}

	@Override
	public abstract void close() throws IOException;

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

	public Response<JsonElement> sendRequest(Request<JsonObject> request)
			throws IOException {
		return rsHelper.sendRequest(request);
	}

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

	public KeepAliveManager getKeepAliveManager() {
		return keepAliveManager;
	}

	public void setKeepAliveManager(KeepAliveManager keepAliveManager) {
		this.keepAliveManager = keepAliveManager;
	}

}
