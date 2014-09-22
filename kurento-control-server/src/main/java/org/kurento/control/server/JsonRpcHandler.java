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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.kurento.client.internal.transport.jsonrpc.JsonResponseUtils;
import org.kurento.control.server.exceptions.KurentoControlServerTransportException;
import org.kurento.control.server.exceptions.ResponsePropagationException;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.KeepAliveManager;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.TransportException;
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 1.0.0
 *
 */
public final class JsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final String OPERATIONS_PROPERTY = "operations";
	private static final String SUBSCRIPTION_PROPERTY = "subscription";
	private static final String TYPE_PROPERTY = "type";
	private static final String OBJECT_PROPERTY = "object";
	private static final String VALUE_PROPERTY = "value";

	private static final String SUBSCRIBE_METHOD = "subscribe";
	private static final String TRANSACTION_METHOD = "transaction";

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

		if (request.getMethod().equals(TRANSACTION_METHOD)) {

			processTransactionRequest(transaction, request);

		} else {
			try {
				sendRequest(transaction, request, true);
			} catch (KurentoControlServerTransportException e) {
				throw new TransportException(e);
			}
		}
	}

	private void sendRequest(final Transaction transaction,
			final Request<JsonObject> request, final boolean retry) {

		try {

			client.sendRequest(request.getMethod(), request.getParams(),
					new Continuation<JsonElement>() {

						@Override
						public void onSuccess(JsonElement result) {
							if (request.getId() != null) {
								processIfSubscribeResponse(
										transaction.getSession(), request,
										result);
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

	private void processIfSubscribeResponse(final Session session,
			final Request<JsonObject> request, JsonElement result) {

		if (request.getMethod().equals(SUBSCRIBE_METHOD)) {

			try {
				String subscription = JsonResponseUtils.convertFromResult(
						result, String.class);

				subsManager.addSubscription(subscription,
						getEventInfo(request.getParams()), session);

			} catch (Exception e) {
				log.error("Error getting subscription on response {}", result,
						e);
			}
		}
	}

	private String getEventInfo(final JsonObject jsonObject) {
		String object = jsonObject.get(OBJECT_PROPERTY).getAsString();
		String type = jsonObject.get(TYPE_PROPERTY).getAsString();
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

			JsonObject value = request.getParams().get(VALUE_PROPERTY)
					.getAsJsonObject();

			Collection<Session> sessions;

			if (value.has(SUBSCRIPTION_PROPERTY)) {

				String subscription = value.get(SUBSCRIPTION_PROPERTY)
						.getAsString().trim();
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

	private void processTransactionRequest(Transaction transaction,
			Request<JsonObject> request) {

		List<JsonElement> operations = new ArrayList<>();
		for (JsonElement operation : (JsonArray) request.getParams().get(
				OPERATIONS_PROPERTY)) {
			operations.add(operation);
		}

		processTransactionOperations(transaction, operations,
				new ArrayList<Response<JsonElement>>(),
				new TransactionManager());
	}

	private void processTransactionOperations(final Transaction transaction,
			final List<JsonElement> operations,
			final List<Response<JsonElement>> responses,
			final TransactionManager txManager) {

		if (operations.isEmpty()) {

			try {
				transaction.sendResponse(responses);
			} catch (IOException e) {
				throw new ResponsePropagationException(
						"Could not send response to client", e);
			}

		} else {

			JsonElement atomicRequestJson = operations.remove(0);

			final Request<JsonObject> atomicRequest = JsonUtils
					.fromJsonRequest((JsonObject) atomicRequestJson,
							JsonObject.class);

			txManager.updateRequest(atomicRequest);

			final Integer origId = atomicRequest.getId();
			atomicRequest.setId(null);

			try {

				client.sendRequest(atomicRequest,
						new Continuation<Response<JsonElement>>() {

							@Override
							public void onSuccess(Response<JsonElement> response) {
								txManager.updateResponse(response);
								processIfSubscribeResponse(
										transaction.getSession(),
										atomicRequest, response.getResult());

								response.setId(origId);
								responses.add(response);
								processTransactionOperations(transaction,
										operations, responses, txManager);
							}

							@Override
							public void onError(Throwable cause) {
								// TODO Add retry
								try {
									transaction.sendError(cause);
								} catch (IOException e) {
									log.error(
											"Could not send response to client",
											e);
								}
							}
						});

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
	}
}
