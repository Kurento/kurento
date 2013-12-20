package com.kurento.kmf.jsonrpcconnector.internal.ws;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.server.TransactionImpl.ResponseSender;

public final class WebSocketResponseSender implements ResponseSender {

	private static final Logger log = LoggerFactory
			.getLogger(WebSocketResponseSender.class);

	private WebSocketSession wsSession;

	public WebSocketResponseSender(WebSocketSession wsSession) {
		this.wsSession = wsSession;
	}

	public void sendResponse(Message message) throws IOException {
		String jsonMessage = message.toString();
		log.info("[Server] Message sent: " + jsonMessage);
		wsSession.sendMessage(new TextMessage(jsonMessage));
	}
}