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
package org.kurento.kmf.jsonrpcconnector.client;

import static org.kurento.kmf.jsonrpcconnector.JsonUtils.fromJsonResponse;
import static org.kurento.kmf.jsonrpcconnector.JsonUtils.toJson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.kurento.kmf.jsonrpcconnector.JsonUtils;
import org.kurento.kmf.jsonrpcconnector.internal.HttpResponseSender;
import org.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import org.kurento.kmf.jsonrpcconnector.internal.client.ClientSession;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;
import org.kurento.kmf.jsonrpcconnector.internal.message.Response;
import org.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;

public class JsonRpcClientHttp extends JsonRpcClient {

	private final Logger log = LoggerFactory.getLogger(JsonRpcClient.class);

	private Thread longPoolingThread;
	private String url;

	private HttpResponseSender rs;

	private final HttpHeaders headers = new HttpHeaders();

	public JsonRpcClientHttp(String url) {
		this(url, new HttpHeaders());
	}

	public JsonRpcClientHttp(String url, HttpHeaders headers) {
		this.url = url;
		this.rs = new HttpResponseSender();
		this.rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				return internalSendRequestHttp(request, resultClass);
			}

			@Override
			protected void internalSendRequest(
					Request<? extends Object> request,
					Class<JsonElement> class1,
					Continuation<Response<JsonElement>> continuation) {
				throw new UnsupportedOperationException(
						"Async client int local is unavailable");
			}
		};

		this.headers.putAll(headers);
	}

	private void updateSession(Response<?> response) {

		String sessionId = response.getSessionId();

		rsHelper.setSessionId(sessionId);

		if (session == null) {
			session = new ClientSession(sessionId, registerInfo, this);

			handlerManager.afterConnectionEstablished(session);

			startPooling();
		}
	}

	private void startPooling() {
		this.longPoolingThread = new Thread() {
			@Override
			public void run() {
				longPooling();
			}
		};

		this.longPoolingThread.start();
	}

	private void longPooling() {

		while (true) {

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.debug("Long polling thread interrupted", e);
			}

			if (Thread.interrupted()) {
				break;
			}

			try {

				JsonElement requestsListJsonObject = this.sendRequest(
						Request.POLL_METHOD_NAME, rs.getResponseListToSend(),
						JsonElement.class);

				log.info("Response from pool: {}", requestsListJsonObject);

				Type collectionType = new TypeToken<List<Request<JsonElement>>>() {
				}.getType();

				List<Request<JsonElement>> requestList = JsonUtils.fromJson(
						requestsListJsonObject, collectionType);

				processServerRequests(requestList);

			} catch (IOException e) {
				// TODO Decide what to do in this case. If the net connection is
				// lost, this will retry indefinitely
				log.error(
						"Exception when waiting for events (long-polling). Retry",
						e);
			}
		}
	}

	private void processServerRequests(List<Request<JsonElement>> requestList) {
		for (Request<JsonElement> request : requestList) {
			try {
				handlerManager.handleRequest(session, request, rs);
			} catch (IOException e) {
				log.error(
						"Exception while processing request from server to client",
						e);
			}
		}
	}

	private <P, R> Response<R> internalSendRequestHttp(Request<P> request,
			Class<R> resultClass) throws IOException {

		String resultJson = org.apache.http.client.fluent.Request.Post(url)
				.bodyString(toJson(request), ContentType.APPLICATION_JSON)
				.execute().returnContent().asString();

		if (resultJson == null || resultJson.trim().isEmpty()) {
			return new Response<>(request.getId(), new ResponseError(3,
					"The server send an empty response"));
		}

		Response<R> response = fromJsonResponse(resultJson, resultClass);

		updateSession(response);

		return response;
	}

	@Override
	public void close() {
		if (this.longPoolingThread != null) {
			log.info("Interrupted!!!");
			this.longPoolingThread.interrupt();
		}
		handlerManager.afterConnectionClosed(session,
				"Client closed connection");
		session = null;
	}

}
