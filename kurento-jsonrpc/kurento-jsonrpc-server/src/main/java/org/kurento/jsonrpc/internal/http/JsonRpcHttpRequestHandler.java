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
package org.kurento.jsonrpc.internal.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.internal.JsonRpcRequestSenderHelper;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.internal.server.ProtocolManager;
import org.kurento.jsonrpc.internal.server.ProtocolManager.ServerSessionFactory;
import org.kurento.jsonrpc.internal.server.ServerSession;
import org.kurento.jsonrpc.internal.server.SessionsManager;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.springframework.web.HttpRequestHandler;

import com.google.common.io.CharStreams;
import com.google.gson.JsonElement;

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
				protected void internalSendRequest(
						Request<? extends Object> request,
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

		@Override
		public void closeNativeSession(String reason) {
			throw new UnsupportedOperationException();			
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

			@Override
			public void updateSessionOnReconnection(ServerSession session) {
				throw new UnsupportedOperationException();
			}
		};

		ResponseSender responseSender = new ResponseSender() {
			@Override
			public void sendResponse(Message message) throws IOException {
				servletResponse.getWriter().println(message);
			}

			@Override
			public void sendPingResponse(Message message) throws IOException {
				sendResponse(message);
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
