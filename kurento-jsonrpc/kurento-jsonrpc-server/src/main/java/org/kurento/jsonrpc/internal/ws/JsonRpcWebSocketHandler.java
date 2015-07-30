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
package org.kurento.jsonrpc.internal.ws;

import java.io.IOException;

import org.kurento.jsonrpc.internal.client.AbstractSession;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.internal.server.ProtocolManager;
import org.kurento.jsonrpc.internal.server.ProtocolManager.ServerSessionFactory;
import org.kurento.jsonrpc.internal.server.ServerSession;
import org.kurento.jsonrpc.internal.server.SessionsManager;
import org.kurento.jsonrpc.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class JsonRpcWebSocketHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcWebSocketHandler.class);

	private final ProtocolManager protocolManager;

	private String label = "";

	public JsonRpcWebSocketHandler(ProtocolManager protocolManager) {
		this.protocolManager = protocolManager;
	}

	public void setLabel(String label) {
		this.label = "[" + label + "] ";
		this.protocolManager.setLabel(label);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception {

		try {
			// We send this notification to the JsonRpcHandler when the JsonRpc
			// session is established, not when websocket session is established
			log.info(
					"{} Client connection stablished from session={} uri={} headers={} acceptedProtocol={} attributes={}",
					label, session.getRemoteAddress(), session.getUri(),
					session.getHandshakeHeaders(),
					session.getAcceptedProtocol(), session.getAttributes());

		} catch (Throwable t) {
			log.error(
					label
							+ "Exception processing afterConnectionEstablished in session={}",
					session.getId(), t);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession wsSession,
			org.springframework.web.socket.CloseStatus status) throws Exception {

		try {
			AbstractSession session = protocolManager.getSessionByTransportId(wsSession.getId());
			
			if(session != null){
				log.info(
					"{} WebSocket session {} with transportId {} closed for {} (code {}, reason '{}')",
					label, session.getSessionId(), wsSession.getId(),
					CloseStatusHelper.getCloseStatusType(status.getCode()),
					status.getCode(), status.getReason());

				protocolManager.closeSessionIfTimeout(wsSession.getId(),
					status.getReason());
			} else {
				log.info("{} WebSocket session not associated to any jsonRpcSession "
						+ "with transportId {} closed for {} (code {}, reason '{}')", label, wsSession.getId(),
						CloseStatusHelper.getCloseStatusType(status.getCode()), status.getCode(), status.getReason());
			}

		} catch (Throwable t) {
			log.error(
					label
							+ "Exception processing afterConnectionClosed in session={}",
					wsSession.getId(), t);
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {

		try {
			protocolManager.processTransportError(session.getId(), exception);
		} catch (Throwable t) {
			log.error(label
					+ "Exception processing transportError in session={}",
					session.getId(), t);
		}
	}

	@Override
	public void handleTextMessage(final WebSocketSession wsSession,
			TextMessage message) throws Exception {

		try {

			String messageJson = message.getPayload();

			// TODO Ensure only one register message per websocket session.
			ServerSessionFactory factory = new ServerSessionFactory() {
				@Override
				public ServerSession createSession(String sessionId,
						Object registerInfo, SessionsManager sessionsManager) {
					return new WebSocketServerSession(sessionId, registerInfo,
							sessionsManager, wsSession);
				}

				@Override
				public void updateSessionOnReconnection(ServerSession session) {
					((WebSocketServerSession) session)
					.updateWebSocketSession(wsSession);
				}
			};

			protocolManager.processMessage(messageJson, factory,
					new ResponseSender() {
				@Override
				public void sendResponse(Message message)
						throws IOException {

					String jsonMessage = message.toString();
					log.debug("{} <-Res {}", label, jsonMessage);
					sendJsonMessage(jsonMessage);
				}

				@Override
				public void sendPingResponse(Message message)
						throws IOException {

					String jsonMessage = message.toString();
					log.trace("{} <-Res {}", label, jsonMessage);
					sendJsonMessage(jsonMessage);
				}

				private void sendJsonMessage(String jsonMessage)
						throws IOException {
					synchronized (wsSession) {
						if (wsSession.isOpen()) {
							wsSession.sendMessage(new TextMessage(
									jsonMessage));
						} else {
							log.error("Trying to send a message to a closed session");
						}
					}
				}
			}, wsSession.getId());

		} catch (Throwable t) {
			log.error(label + "Exception processing request", t);
		}

	}

}
