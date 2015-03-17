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

import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_PING;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.METHOD_RECONNECT;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.PONG;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.PONG_PAYLOAD;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.RECONNECTION_ERROR;
import static org.kurento.jsonrpc.internal.JsonRpcConstants.RECONNECTION_SUCCESSFUL;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.kurento.commons.SecretGenerator;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

public class ProtocolManager {

	public interface ServerSessionFactory {
		ServerSession createSession(String sessionId, Object registerInfo,
				SessionsManager sessionsManager);

		void updateSessionOnReconnection(ServerSession session);
	}

	private static final Logger log = LoggerFactory
			.getLogger(ProtocolManager.class);

	protected SecretGenerator secretGenerator = new SecretGenerator();

	@Autowired
	private SessionsManager sessionsManager;

	@Autowired
	@Qualifier("jsonrpcTaskScheduler")
	private TaskScheduler taskScheduler;

	private final JsonRpcHandlerManager handlerManager;

	private String label = "";

	private int maxHeartbeats = 0;

	private int heartbeats = 0;

	public ProtocolManager(JsonRpcHandler<?> handler) {
		this.handlerManager = new JsonRpcHandlerManager(handler);
	}

	public ProtocolManager(JsonRpcHandler<?> handler,
			SessionsManager sessionsManager, TaskScheduler taskScheduler) {
		this.handlerManager = new JsonRpcHandlerManager(handler);
		this.sessionsManager = sessionsManager;
		this.taskScheduler = taskScheduler;
	}

	public void setLabel(String label) {
		this.label = "[" + label + "] ";
	}

	public void processMessage(String messageJson,
			ServerSessionFactory factory, ResponseSender responseSender,
			String internalSessionId) throws IOException {

		JsonObject messagetJsonObject = JsonUtils.fromJson(messageJson,
				JsonObject.class);

		processMessage(messagetJsonObject, factory, responseSender,
				internalSessionId);
	}

	/**
	 * Process incoming message. The response is sent using responseSender. If
	 * null, the session will be used.
	 *
	 * @param messageJson
	 * @param factory
	 * @param responseSender
	 * @param internalSessionId
	 * @throws IOException
	 */
	public void processMessage(JsonObject messagetJsonObject,
			ServerSessionFactory factory, ResponseSender responseSender,
			String internalSessionId) throws IOException {

		if (messagetJsonObject.has(Request.METHOD_FIELD_NAME)) {
			processRequestMessage(factory, messagetJsonObject, responseSender,
					internalSessionId);
		} else {
			processResponseMessage(messagetJsonObject, internalSessionId);
		}
	}

	// TODO Unify ServerSessionFactory, ResponseSender and transportId in a
	// entity "RequestContext" or similar. In this way, there are less
	// parameters
	// and the implementation is easier
	private void processRequestMessage(ServerSessionFactory factory,
			JsonObject requestJsonObject, ResponseSender responseSender,
			String transportId) throws IOException {

		Request<JsonElement> request = JsonUtils.fromJsonRequest(
				requestJsonObject, JsonElement.class);

		switch (request.getMethod()) {
		case METHOD_RECONNECT:

			log.debug("{} Req-> {}", label, request);
			processReconnectMessage(factory, request, responseSender,
					transportId);
			break;
		case METHOD_PING:
			log.trace("{} Req-> {}", label, request);
			processPingMessage(factory, request, responseSender, transportId);

			break;
		default:

			log.debug("{} Req-> {}", label, request);
			ServerSession session = getSession(factory, transportId, request);

			// TODO, Take out this an put in Http specific handler. The main
			// reason is to wait for request before responding to the client.
			// And for no contaminate the ProtocolManager.
			if (request.getMethod().equals(Request.POLL_METHOD_NAME)) {

				Type collectionType = new TypeToken<List<Response<JsonElement>>>() {
				}.getType();

				List<Response<JsonElement>> responseList = JsonUtils.fromJson(
						request.getParams(), collectionType);

				for (Response<JsonElement> response : responseList) {
					session.handleResponse(response);
				}

				// Wait for some time if there is a request from server to
				// client

				// TODO Allow send empty responses. Now you have to send at
				// least an
				// empty string
				responseSender.sendResponse(new Response<Object>(request
						.getId(), Collections.emptyList()));

			} else {
				handlerManager.handleRequest(session, request, responseSender);
			}
			break;
		}

	}

	private ServerSession getSession(ServerSessionFactory factory,
			String transportId, Request<JsonElement> request) {

		ServerSession session = null;

		if (request.getSessionId() != null) {
			session = sessionsManager.get(request.getSessionId());

			if (session == null) {
				log.warn(label + "There is no session with specified id '{}'."
						+ "Creating a new one.", request.getSessionId());
			}

		} else if (transportId != null) {
			session = sessionsManager.getByTransportId(transportId);
		}

		if (session == null) {
			session = createSession(factory, session);
			handlerManager.afterConnectionEstablished(session);
		} else {
			session.setNew(false);
		}

		return session;
	}

	private void processPingMessage(ServerSessionFactory factory,
			Request<JsonElement> request, ResponseSender responseSender,
			String transportId) throws IOException {
		if (maxHeartbeats == 0 || maxHeartbeats > ++heartbeats) {
			String sessionId = request.getSessionId();
			JsonObject pongPayload = new JsonObject();
			pongPayload.add(PONG_PAYLOAD, new JsonPrimitive(PONG));
			responseSender.sendPingResponse(new Response<>(sessionId, request
					.getId(), pongPayload));
		}
	}

	private void processReconnectMessage(ServerSessionFactory factory,
			Request<JsonElement> request, ResponseSender responseSender,
			String transportId) throws IOException {

		String sessionId = request.getSessionId();

		if (sessionId == null) {

			responseSender
					.sendResponse(new Response<>(
							request.getId(),
							new ResponseError(99999,
									"SessionId is mandatory in a reconnection request")));
		} else {

			ServerSession session = sessionsManager.get(sessionId);
			if (session != null) {

				String oldTransportId = session.getTransportId();
				session.setTransportId(transportId);
				factory.updateSessionOnReconnection(session);
				sessionsManager.updateTransportId(session, oldTransportId);

				// FIXME: Possible race condition if session is disposed when
				// reconnect method has arrived
				cancelCloseTimer(session);

				responseSender.sendResponse(new Response<>(sessionId, request
						.getId(), RECONNECTION_SUCCESSFUL));
			} else {

				responseSender.sendResponse(new Response<>(request.getId(),
						new ResponseError(99999, RECONNECTION_ERROR)));
			}
		}
	}

	private ServerSession createSession(ServerSessionFactory factory,
			Object registerInfo) {

		String sessionId = secretGenerator.nextSecret();

		ServerSession session = factory.createSession(sessionId, registerInfo,
				sessionsManager);

		sessionsManager.put(session);

		return session;
	}

	private void processResponseMessage(JsonObject messagetJsonObject,
			String internalSessionId) {

		Response<JsonElement> response = JsonUtils.fromJsonResponse(
				messagetJsonObject, JsonElement.class);

		ServerSession session = sessionsManager
				.getByTransportId(internalSessionId);

		session.handleResponse(response);
	}

	public void closeSessionIfTimeout(final String transportId,
			final String reason) {

		final ServerSession session = sessionsManager
				.getByTransportId(transportId);

		if (session != null) {

			log.info(label + "Configuring close timeout for session: {}",
					session.getSessionId());

			try {

				ScheduledFuture<?> lastStartedTimerFuture = taskScheduler
						.schedule(
								new Runnable() {
									@Override
									public void run() {
										closeSession(session, reason);
									}
								},
								new Date(
										System.currentTimeMillis()
												+ session
														.getReconnectionTimeoutInMillis()));

				session.setCloseTimerTask(lastStartedTimerFuture);

			} catch (TaskRejectedException e) {
				log.warn(label + "Close timeout for session {} can not be set "
						+ "because the scheduler is shutdown",
						session.getSessionId());
			}
		}
	}

	public void closeSession(ServerSession session, String reason) {
		log.info(label + "Closing session: {}", session.getSessionId());
		sessionsManager.remove(session);
		handlerManager.afterConnectionClosed(session, reason);
	}

	public void cancelCloseTimer(ServerSession session) {
		if (session.getCloseTimerTask() != null) {
			session.getCloseTimerTask().cancel(false);
		}
	}

	public void processTransportError(String transportId, Throwable exception) {
		final ServerSession session = sessionsManager
				.getByTransportId(transportId);
		handlerManager.handleTransportError(session, exception);
	}

	/**
	 * Method intended to be used for testing purposes
	 *
	 * @param maxHeartbeats
	 */
	public void setMaxNumberOfHeartbeats(int maxHeartbeats) {
		this.maxHeartbeats = maxHeartbeats;
	}
}
