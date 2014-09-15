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
package org.kurento.control.server;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.kurento.control.server.exceptions.KurentoControlServerTransportException;
import org.kurento.control.server.exceptions.ResponsePropagationException;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.KeepAliveManager;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.TransportException;
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 1.0.0
 *
 */
public final class JsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcHandler.class);

	@Autowired
	private JsonRpcClient client;

	private final SubscriptionsManager subsManager = new SubscriptionsManager();

	private KeepAliveManager keepAliveManager;

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

		if (keepAliveManager != null) {
			keepAliveManager.addId(session.getSessionId());
		}
	}

	@Override
	public void afterConnectionClosed(Session session, String status)
			throws Exception {

		subsManager.removeSession(session);

		if (keepAliveManager != null) {
			keepAliveManager.removeId(session.getSessionId());
		}
	}

	@Override
	public void handleRequest(final Transaction transaction,
			final Request<JsonObject> request) throws Exception {

		transaction.startAsync();
		try {
			sendRequest(transaction, request, true);
		} catch (KurentoControlServerTransportException e) {
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
			throw new KurentoControlServerTransportException(
					"Exception while executing a command"
							+ " in Kurento Media Server", e);
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
