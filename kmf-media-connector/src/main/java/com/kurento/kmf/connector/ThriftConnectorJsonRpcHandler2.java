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
package com.kurento.kmf.connector;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.connector.exceptions.MediaConnectorTransportException;
import com.kurento.kmf.connector.exceptions.ResponsePropagationException;
import com.kurento.kmf.jsonrpcconnector.*;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 1.0.0
 *
 */
public final class ThriftConnectorJsonRpcHandler2 extends
		DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory
			.getLogger(ThriftConnectorJsonRpcHandler2.class);

	@Autowired
	private JsonRpcClient client;

	private final ConcurrentMap<String, Session> subscriptions = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		client.setServerRequestHandler(new DefaultJsonRpcHandler<JsonObject>() {

			@Override
			public void handleRequest(Transaction transaction,
					Request<JsonObject> request) throws Exception {
				internalEventJsonRpc(request);
			}
		});
	}

	@PreDestroy
	private void destroy() throws IOException {
		if (client != null) {
			client.close();
		}
	}

	@Override
	public void afterConnectionEstablished(final Session session)
			throws Exception {

	}

	@Override
	public void afterConnectionClosed(Session session, String status)
			throws Exception {

		Iterator<Entry<String, Session>> it = subscriptions.entrySet()
				.iterator();

		while (it.hasNext()) {
			Entry<String, Session> value = it.next();
			if (value.getValue() == session) {
				it.remove();
			}
		}
	}

	@Override
	public void handleRequest(final Transaction transaction,
			final Request<JsonObject> request) throws Exception {

		transaction.startAsync();
		try {
			sendRequest(transaction, request, true);
		} catch (MediaConnectorTransportException e) {
			throw new TransportException(e);
		}
	}

	private void sendRequest(final Transaction transaction,
			final Request<JsonObject> request, final boolean retry) {

		final boolean subscribeRequest;

		if (request.getMethod().equals("subscribe")) {
			subscribeRequest = true;
		} else {
			subscribeRequest = false;
		}

		try {
			client.sendRequest(request.getMethod(), request.getParams(),
					new Continuation<JsonElement>() {

						@Override
						public void onSuccess(JsonElement result) {
							if (request.getId() != null) {
								requestOnComplete(result, transaction,
										subscribeRequest);
							}
						}

						@Override
						public void onError(Throwable cause) {

							log.error("Error sending request " + request, cause);
							if (retry && cause instanceof ConnectException) {
								sendRequest(transaction, request, false);
							} else {
								requestOnError(cause, transaction);
							}
						}
					});

		} catch (Exception e) {
			throw new MediaConnectorTransportException(
					"Exception while executing a command"
							+ " in thrift interface of the MediaServer", e);
		}
	}

	private void requestOnError(Throwable exception, Transaction transaction) {

		try {

			if (exception instanceof JsonRpcErrorException) {

				JsonRpcErrorException error = (JsonRpcErrorException) exception;

				transaction.sendError(error.getCode(), error.getMessage(),
						error.getData());

			} else {

				transaction.sendError(exception);
			}

		} catch (IOException e) {
			throw new ResponsePropagationException(
					"Exception while sending response to client", e);
		}
	}

	private void requestOnComplete(JsonElement result, Transaction transaction,
			boolean subscribeResponse) {

		try {

			if (subscribeResponse) {
				try {
					String subscription = ((JsonObject) result).get("value")
							.getAsString().trim();
					subscriptions.put(subscription, transaction.getSession());
				} catch (Exception e) {
					log.error("Error getting subscription on response {}",
							result, e);
				}
			}

			transaction.sendResponse(result);

		} catch (IOException e) {

			try {
				transaction.sendError(e);
			} catch (IOException e1) {
				throw new ResponsePropagationException(
						"Could not notify client that an exception was produced getting the result from a media server response",
						e1);
			}
		}
	}

	private void internalEventJsonRpc(Request<JsonObject> request) {
		try {

			log.debug("<-Not {}", request);

			JsonElement subsJsonElem = request.getParams().get("value")
					.getAsJsonObject().get("subscription");

			if (subsJsonElem == null) {
				log.error("Received event wihthout subscription: {}", request);
				return;
			}

			String subscription = subsJsonElem.getAsString().trim();
			Session session = subscriptions.get(subscription);
			if (session == null) {
				log.error("Unknown event subscription: '{}'", subscription);
				return;
			}

			try {

				session.sendNotification("onEvent", request.getParams());

			} catch (IOException e) {
				log.error(
						"Exception while sending event from KMS to the client",
						e);
			}

		} catch (Exception e) {
			log.error("Exception processing server event", e);
		}
	}
}
