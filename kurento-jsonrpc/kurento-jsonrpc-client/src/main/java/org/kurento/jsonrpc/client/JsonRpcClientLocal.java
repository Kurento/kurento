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

import static javax.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE;

import java.io.IOException;

import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.kurento.jsonrpc.internal.client.TransactionImpl;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonRpcClientLocal extends JsonRpcClient {

	private static Logger log = LoggerFactory
			.getLogger(JsonRpcClientLocal.class);

	private JsonRpcHandler<? extends Object> remoteHandler;
	private final JsonRpcHandlerManager remoteHandlerManager = new JsonRpcHandlerManager();

	public <F> JsonRpcClientLocal(JsonRpcHandler<? extends Object> handler) {

		this.remoteHandler = handler;
		this.remoteHandlerManager.setJsonRpcHandler(remoteHandler);

		session = new ClientSession("XXX", null, this);

		rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				return localSendRequest(request, resultClass);
			}

			@Override
			protected void internalSendRequest(
					Request<? extends Object> request,
					Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {
				Response<JsonElement> result = localSendRequest(request,
						resultClass);
				if (result != null) {
					continuation.onSuccess(result);
				}
			}
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <R, P> Response<R> localSendRequest(Request<P> request,
			Class<R> resultClass) {
		// Simulate sending json string for net
		String jsonRequest = request.toString();

		log.debug("--> {}", jsonRequest);

		Request<JsonObject> newRequest = JsonUtils.fromJsonRequest(jsonRequest,
				JsonObject.class);

		final Response<JsonObject>[] response = new Response[1];

		ClientSession clientSession = new ClientSession("XXX", null,
				new JsonRpcRequestSenderHelper() {

					@Override
					protected void internalSendRequest(
							Request<? extends Object> request,
							Class<JsonElement> clazz,
							final Continuation<Response<JsonElement>> continuation) {
						try {
							handlerManager.handleRequest(session,
									(Request<JsonElement>) request,
									new ResponseSender() {
										@Override
										public void sendResponse(Message message)
												throws IOException {
											continuation
													.onSuccess((Response<JsonElement>) message);
										}
									});
						} catch (IOException e) {
							continuation.onError(e);
						}
					}

					@Override
					protected <P2, R2> Response<R2> internalSendRequest(
							Request<P2> request, Class<R2> resultClass)
							throws IOException {

						final Object[] response = new Object[1];
						try {
							handlerManager.handleRequest(session,
									(Request<JsonElement>) request,
									new ResponseSender() {
										@Override
										public void sendResponse(Message message)
												throws IOException {
											response[0] = message;
										}
									});

							Response<R2> response2 = (Response<R2>) response[0];
							Object result = response2.getResult();

							if (result == null
									|| resultClass.isAssignableFrom(result
											.getClass())) {
								return response2;
							} else if (resultClass == JsonElement.class) {
								response2.setResult((R2) JsonUtils
										.toJsonElement(result));
								return response2;
							} else {
								throw new ClassCastException("Class " + result
										+ " cannot be converted to "
										+ resultClass);
							}

						} catch (IOException e) {
							return new Response<R2>(request.getId(),
									ResponseError.newFromException(e));
						}
					}
				});

		TransactionImpl t = new TransactionImpl(clientSession, newRequest,
				new ResponseSender() {

					@Override
					public void sendResponse(Message message)
							throws IOException {
						response[0] = (Response<JsonObject>) message;
					}
				});

		try {
			remoteHandler.handleRequest(t, (Request) request);
		} catch (Exception e) {
			ResponseError error = ResponseError.newFromException(e);
			return new Response<>(request.getId(), error);
		}

		if (response[0] != null) {
			// Simulate receiving json string from net
			Response<R> responseObj = (Response<R>) response[0];
			if (responseObj.getId() == null) {
				responseObj.setId(request.getId());
			}
			String jsonResponse = responseObj.toString();

			// log.debug("< {}", jsonResponse);

			Response<R> newResponse = JsonUtils.fromJsonResponse(jsonResponse,
					resultClass);

			newResponse.setId(request.getId());

			return newResponse;

		}

		return new Response<>(request.getId());

	}

	@Override
	public void close() throws IOException {
		handlerManager
				.afterConnectionClosed(session, NORMAL_CLOSURE.toString());
	}

	@Override
	public void setServerRequestHandler(
			org.kurento.jsonrpc.JsonRpcHandler<?> handler) {
		super.setServerRequestHandler(handler);
		handlerManager.afterConnectionEstablished(session);
		remoteHandlerManager.afterConnectionEstablished(session);
	}

	@Override
	public void connect() throws IOException {
		// TODO Auto-generated method stub

	}

}
