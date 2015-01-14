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

import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public final class WebSocketResponseSender implements ResponseSender {

	private static final Logger log = LoggerFactory
			.getLogger(WebSocketResponseSender.class);

	private final WebSocketSession wsSession;

	public WebSocketResponseSender(WebSocketSession wsSession) {
		this.wsSession = wsSession;
	}

	@Override
	public void sendResponse(Message message) throws IOException {
		String jsonMessage = message.toString();
		log.debug("<-Res {}", jsonMessage);
		synchronized (wsSession) {
			if (wsSession.isOpen()) {
				wsSession.sendMessage(new TextMessage(jsonMessage));
			} else {
				log.error("Trying to send a message to a closed session");
			}
		}
	}
}