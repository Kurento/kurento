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
package org.kurento.jsonrpcconnector.internal.server;

import static org.kurento.jsonrpcconnector.internal.JsonRpcConstants.METHOD_RECONNECT;
import static org.kurento.jsonrpcconnector.internal.JsonRpcConstants.RECONNECTION_ERROR;
import static org.kurento.jsonrpcconnector.internal.JsonRpcConstants.RECONNECTION_SUCCESSFUL;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.kurento.common.SecretGenerator;
import org.kurento.jsonrpcconnector.JsonRpcHandler;
import org.kurento.jsonrpcconnector.JsonUtils;
import org.kurento.jsonrpcconnector.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpcconnector.internal.message.Request;
import org.kurento.jsonrpcconnector.internal.message.Response;
import org.kurento.jsonrpcconnector.internal.message.ResponseError;

public class ProtocolManager {

	public interface ServerSessionFactory {
		ServerSession createSession(String sessionId, Object registerInfo,
				SessionsManager sessionsManager);
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

	public ProtocolManager(JsonRpcHandler<?> handler) {
		this.handlerManager = new JsonRpcHandlerManager(handler);
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
	public void processMessage(String messageJson,
			ServerSessionFactory factory, ResponseSender responseSender,
			String internalSessionId) throws IOException {

		JsonObject messagetJsonObject = JsonUtils.fromJson(messageJson,
				JsonObject.class);

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

		if (request.getMethod().equals(METHOD_RECONNECT)) {

			processReconnectMessage(factory, request, responseSender,
					transportId);

		} else {

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
		}
	}

	private ServerSession getSession(ServerSessionFactory factory,
			String transportId, Request<JsonElement> request) {

		ServerSession session = null;

		if (request.getSessionId() != null) {
			session = sessionsManager.get(request.getSessionId());

			if (session == null) {
				log.warn("There is no session with specified id '{}'."
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
				sessionsManager.updateTransportId(session, oldTransportId);

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

			log.info("Configuring close timeout for session: {}",
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
				log.warn("Close timeout for session {} can not be set "
						+ "because the scheduler is shutdown",
						session.getSessionId());
			}
		}
	}

	public void closeSession(ServerSession session, String reason) {
		log.info("Closing session: {}", session.getSessionId());
		sessionsManager.remove(session);
		handlerManager.afterConnectionClosed(session, reason);
	}

	public void cancelCloseTimer(ServerSession session) {
		if (session.getCloseTimerTask() != null) {
			session.getCloseTimerTask().cancel(false);
		}
	}
}
