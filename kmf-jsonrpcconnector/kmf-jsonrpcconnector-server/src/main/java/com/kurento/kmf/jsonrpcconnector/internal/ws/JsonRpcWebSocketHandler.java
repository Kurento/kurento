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
package com.kurento.kmf.jsonrpcconnector.internal.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.kurento.kmf.jsonrpcconnector.internal.server.ProtocolManager;
import com.kurento.kmf.jsonrpcconnector.internal.server.ProtocolManager.ServerSessionFactory;
import com.kurento.kmf.jsonrpcconnector.internal.server.ServerSession;
import com.kurento.kmf.jsonrpcconnector.internal.server.SessionsManager;

public class JsonRpcWebSocketHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcWebSocketHandler.class);

	private final ProtocolManager protocolManager;

	public JsonRpcWebSocketHandler(ProtocolManager protocolManager) {
		this.protocolManager = protocolManager;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception {

		// We send this notification to the JsonRpcHandler when the JsonRpc
		// session is established, not when websocket session is established
	}

	@Override
	public void afterConnectionClosed(WebSocketSession wsSession,
			org.springframework.web.socket.CloseStatus status) throws Exception {

		log.info("Connection closed because: " + status);
		if(!status.equals(CloseStatus.NORMAL)){
			log.error("Abnormal termination");
		} else {
			log.info("Normal termination");
		}

		protocolManager.closeSessionIfTimeout(wsSession.getId(),
				status.getReason());
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		// TODO What to do here?
	}

	@Override
	public void handleTextMessage(final WebSocketSession wsSession,
			TextMessage message) throws Exception {

		String messageJson = message.getPayload();

		log.info("--> {}", messageJson);

		// TODO Ensure only one register message per websocket session.
		ServerSessionFactory factory = new ServerSessionFactory() {
			@Override
			public ServerSession createSession(String sessionId,
					Object registerInfo, SessionsManager sessionsManager) {
				return new WebSocketServerSession(sessionId, registerInfo,
						sessionsManager, wsSession);
			}
		};

		protocolManager.processMessage(messageJson, factory,
				new WebSocketResponseSender(wsSession), wsSession.getId());

	}

}
