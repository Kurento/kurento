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
package org.kurento.client.test;

import static org.junit.Assert.fail;

import java.net.URI;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;
import org.kurento.test.services.KurentoMediaServerManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientToKmsConnectionTest {

	private static Logger log = LoggerFactory
			.getLogger(WebSocketClientToKmsConnectionTest.class);

	@WebSocket
	public class WebSocketHandler {

		@OnWebSocketClose
		public void onClose(int statusCode, String reason) {
			log.debug("WebSocket OnClose");
		}

		@OnWebSocketConnect
		public void onConnect(Session session) {
			log.debug("WebSocket OnConnect");
		}

		@OnWebSocketMessage
		public void onMessage(String msg) {
			log.debug("WebSocket OnMessage: " + msg);
		}
	}

	@Test
	public void reconnectTest() throws Exception {

		for (int i = 0; i < 2; i++) {

			KurentoMediaServerManager kms = KurentoServicesTestHelper
					.startKurentoMediaServer(false);

			String kmsUrl = kms.getLocalhostWsUrl();

			log.info("Connecting to KMS in " + kmsUrl);

			WebSocketClient client = new WebSocketClient();
			WebSocketHandler socket = new WebSocketHandler();

			client.start();
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			Session wsSession = client.connect(socket, new URI(kmsUrl), request)
					.get();

			wsSession.getRemote().sendString("xxxx");

			kms.destroy();

			Thread.sleep(3000);

		}
	}

	@Test
	public void errorSendingClosedKmsTest() throws Exception {

		KurentoMediaServerManager kms = KurentoServicesTestHelper
				.startKurentoMediaServer(false);

		String kmsUrl = kms.getLocalhostWsUrl();

		log.info("Connecting to KMS in " + kmsUrl);

		WebSocketClient client = new WebSocketClient();
		WebSocketHandler socket = new WebSocketHandler();

		client.start();
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		Session wsSession = client.connect(socket, new URI(kmsUrl), request)
				.get();

		wsSession.getRemote().sendString("xxxx");

		kms.destroy();

		Thread.sleep(3000);

		try {

			wsSession.getRemote().sendString("xxxx");
			fail("Trying to send to a closed WebSocket should raise an exception");
		} catch (Exception e) {

		}
	}
}
