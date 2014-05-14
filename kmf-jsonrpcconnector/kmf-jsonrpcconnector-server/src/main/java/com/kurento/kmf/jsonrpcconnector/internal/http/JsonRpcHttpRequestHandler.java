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
package com.kurento.kmf.jsonrpcconnector.internal.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.HttpRequestHandler;

import com.google.common.io.CharStreams;
import com.google.gson.JsonElement;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.server.ProtocolManager;
import com.kurento.kmf.jsonrpcconnector.internal.server.ProtocolManager.ServerSessionFactory;
import com.kurento.kmf.jsonrpcconnector.internal.server.ServerSession;
import com.kurento.kmf.jsonrpcconnector.internal.server.SessionsManager;

public class JsonRpcHttpRequestHandler implements HttpRequestHandler {

	private final class HttpRequestServerSession extends ServerSession {

		private HttpRequestServerSession(String sessionId, Object registerInfo,
				SessionsManager sessionsManager, String internalSessionId) {

			super(sessionId, registerInfo, sessionsManager, internalSessionId);

			setRsHelper(new JsonRpcRequestSenderHelper(sessionId) {

				@Override
				protected <P, R> Response<R> internalSendRequest(
						Request<P> request, Class<R> resultClass)
						throws IOException {
					// TODO Poner aquí la cola de mensajes que devolver al
					// cliente cuando haga pooling
					return new Response<>();
				}

				@Override
				protected void internalSendRequest(Request<Object> request,
						Class<JsonElement> class1,
						Continuation<Response<JsonElement>> continuation) {
					throw new UnsupportedOperationException(
							"Async client is unavailable");
				}
			});
		}

		@Override
		public void handleResponse(Response<JsonElement> response) {
		}
	}

	private final ProtocolManager protocolManager;

	public JsonRpcHttpRequestHandler(ProtocolManager protocolManager) {
		this.protocolManager = protocolManager;
	}

	@Override
	public void handleRequest(HttpServletRequest servletRequest,
			final HttpServletResponse servletResponse) throws ServletException,
			IOException {

		String messageJson = getBodyAsString(servletRequest);

		ServerSessionFactory factory = new ServerSessionFactory() {
			@Override
			public ServerSession createSession(String sessionId,
					Object registerInfo, SessionsManager sessionsManager) {

				return new HttpRequestServerSession(sessionId, registerInfo,
						sessionsManager, null);
			}
		};

		ResponseSender responseSender = new ResponseSender() {
			@Override
			public void sendResponse(Message message) throws IOException {
				servletResponse.getWriter().println(message);
			}
		};

		String internalSessionId = null;

		HttpSession session = servletRequest.getSession(false);
		if (session != null) {
			internalSessionId = session.getId();
		}

		protocolManager.processMessage(messageJson, factory, responseSender,
				internalSessionId);
	}

	/**
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
	private String getBodyAsString(final HttpServletRequest request)
			throws IOException {
		return CharStreams.toString(request.getReader());
	}

}
