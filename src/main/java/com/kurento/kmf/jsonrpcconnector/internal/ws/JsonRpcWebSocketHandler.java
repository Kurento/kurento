package com.kurento.kmf.jsonrpcconnector.internal.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private ProtocolManager protocolManager;

	public JsonRpcWebSocketHandler(ProtocolManager protocolManager) {
		this.protocolManager = protocolManager;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception {

		// We send this notification to the JsonRpcHandler when the JsonRpc
		// session is established, not when websocket session is established
	}

	public void afterConnectionClosed(WebSocketSession wsSession,
			org.springframework.web.socket.CloseStatus status) throws Exception {
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

		log.info("[Server] Message received: " + messageJson);

		// TODO Ensure only one register message per websocket session.
		ServerSessionFactory factory = new ServerSessionFactory() {
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
