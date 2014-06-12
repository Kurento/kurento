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
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.connector.exceptions.MediaConnectorTransportException;
import com.kurento.kmf.connector.exceptions.ResponsePropagationException;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonRpcErrorException;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.TransportException;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 1.0.0
 *
 */
public final class MediaConnectorJsonRpcHandler extends
		DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory
			.getLogger(MediaConnectorJsonRpcHandler.class);

	@Autowired
	private JsonRpcClient client;

	private final SubscriptionsManager subsManager = new SubscriptionsManager();

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
		subsManager.removeSession(session);
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

		final String subsObjectAndType;

		if (request.getMethod().equals("subscribe")) {
			subsObjectAndType = getEventInfo(request.getParams());
		} else {
			subsObjectAndType = null;
		}

		try {
			client.sendRequest(request.getMethod(), request.getParams(),
					new Continuation<JsonElement>() {

						@Override
						public void onSuccess(JsonElement result) {
							if (request.getId() != null) {

								if (subsObjectAndType != null) {
									try {
										String subscription = ((JsonObject) result)
												.get("value").getAsString()
												.trim();

										subsManager.addSubscription(
												subscription,
												subsObjectAndType,
												transaction.getSession());

									} catch (Exception e) {
										log.error(
												"Error getting subscription on response {}",
												result, e);
									}
								}

								requestOnComplete(result, transaction);
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

	private String getEventInfo(final JsonObject jsonObject) {
		String object = jsonObject.get("object").getAsString();
		String type = jsonObject.get("type").getAsString();
		return object + "/" + type;
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

	private void requestOnComplete(JsonElement result, Transaction transaction) {

		try {

			transaction.sendResponse(result);

		} catch (IOException e) {

			try {
				transaction.sendError(e);
			} catch (IOException e1) {
				throw new ResponsePropagationException(
						"Could not notify client that an exception was "
								+ "produced getting the result from a media server response",
						e1);
			}
		}
	}

	private void internalEventJsonRpc(Request<JsonObject> request) {
		try {

			log.debug("<-Not {}", request);

			JsonObject value = request.getParams().get("value")
					.getAsJsonObject();

			Collection<Session> sessions;

			if (value.has("subscription")) {

				String subscription = value.get("subscription").getAsString()
						.trim();
				sessions = subsManager.getSessionsBySubscription(subscription);

			} else {
				String subsObjectAndType = getEventInfo(value);
				sessions = subsManager
						.getSessionsByObjAndType(subsObjectAndType);
			}

			if (!sessions.isEmpty()) {
				for (Session session : sessions) {
					sendNotificationToClient(request, session);
				}
			} else {
				log.error("Received event but no client interested in it: {}",
						request);
				return;
			}

		} catch (Exception e) {
			log.error("Exception processing server event", e);
		}
	}

	private void sendNotificationToClient(Request<JsonObject> request,
			Session session) {

		try {
			session.sendNotification("onEvent", request.getParams());
		} catch (IOException e) {
			log.error("Exception while sending event from KMS to the client", e);
		}
	}
}
