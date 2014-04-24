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
package com.kurento.kmf.jsonrpcconnector.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.client.ClientSession;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;

public class JsonRpcClientLocal extends JsonRpcClient {

	private static Logger LOG = LoggerFactory
			.getLogger(JsonRpcClientLocal.class);

	private JsonRpcHandler<JsonObject> handler;

	public <F> JsonRpcClientLocal(JsonRpcHandler<JsonObject> paramHandler) {

		this.handler = paramHandler;

		session = new ClientSession("XXX", null, this);

		rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				return localSendRequest(request, resultClass);
			}

			@Override
			protected void internalSendRequest(Request<Object> request,
					Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {
				Response<JsonElement> result = localSendRequest(request,
						resultClass);
				continuation.onSuccess(result);
			}
		};
	}

	@SuppressWarnings("unchecked")
	private <R, P> Response<R> localSendRequest(Request<P> request,
			Class<R> resultClass) {
		// Simulate sending json string for net
		String jsonRequest = request.toString();

		LOG.debug("--> {}", jsonRequest);

		Request<JsonObject> newRequest = JsonUtils.fromJsonRequest(jsonRequest,
				JsonObject.class);

		final Response<JsonObject>[] response = new Response[1];

		TransactionImpl t = new TransactionImpl(session, newRequest,
				new ResponseSender() {

					@Override
					public void sendResponse(Message message)
							throws IOException {
						response[0] = (Response<JsonObject>) message;
					}
				});

		try {
			handler.handleRequest(t, (Request<JsonObject>) request);
		} catch (Exception e) {

			ResponseError error = ResponseError.newFromException(e);
			return new Response<>(request.getId(), error);
		}

		if (response[0] != null) {
			// Simulate receiving json string from net
			String jsonResponse = response[0].toString();

			// LOG.debug("< " + jsonResponse);

			Response<R> newResponse = JsonUtils.fromJsonResponse(jsonResponse,
					resultClass);

			newResponse.setId(request.getId());

			return newResponse;

		} else {
			return new Response<>(request.getId());
		}
	}

	@Override
	public void close() throws IOException {

	}

}
