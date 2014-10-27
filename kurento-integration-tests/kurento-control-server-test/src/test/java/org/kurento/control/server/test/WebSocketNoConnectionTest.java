package org.kurento.control.server.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.KurentoControlServerTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(KurentoControlServerTests.class)
public class WebSocketNoConnectionTest {

	private static final Logger log = LoggerFactory
			.getLogger(WebSocketNoConnectionTest.class);

	@ClientEndpoint
	public static class WebSocketClient {

		@OnOpen
		public void onOpen(final Session session) {
			log.info("Connected ... " + session.getId());
		}

		@OnMessage
		public void onMessage(String message, Session session) {
			log.info("Message received: " + message);
		}

		@OnClose
		public void onClose(Session session, CloseReason closeReason) {
			log.info(String.format("Session close %s because of %s",
					session.getId(), closeReason));
		}
	}

	@Test
	public void noConnectionTestStandardAPI() throws IOException {

		try {

			String uri = "ws://127.0.0.1:9999/kurento";

			log.info("Connecting to: " + uri);

			javax.websocket.WebSocketContainer container = javax.websocket.ContainerProvider
					.getWebSocketContainer();

			container.connectToServer(WebSocketClient.class, new URI(uri));

		} catch (DeploymentException | URISyntaxException e) {

			assertThat(e, is(instanceOf(DeploymentException.class)));
			assertThat(
					e.getMessage(),
					containsString("The HTTP request to initiate the WebSocket connection failed"));
		}
	}
}
