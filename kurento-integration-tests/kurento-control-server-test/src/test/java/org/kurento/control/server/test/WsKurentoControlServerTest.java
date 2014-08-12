package org.kurento.control.server.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import javax.websocket.DeploymentException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.testing.KurentoControlServerTests;
import org.kurento.control.server.KurentoControlServerApp;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

@Category(KurentoControlServerTests.class)
public class WsKurentoControlServerTest {

	private static final Logger log = LoggerFactory
			.getLogger(WsKurentoControlServerTest.class);

	@Test
	public void configureWsPortTest() throws IOException, InterruptedException {

		String port = "5555";
		String path = "pathtest";

		System.setProperty(KurentoControlServerApp.WEBSOCKET_PORT_PROPERTY,
				port);
		System.setProperty(KurentoControlServerApp.WEBSOCKET_PATH_PROPERTY,
				path);

		ConfigurableApplicationContext context = KurentoControlServerApp
				.start();

		testClientConnection("ws://127.0.0.1:" + port + "/" + path);

		try {
			testClientConnection("ws://127.0.0.1:" + (port + 2) + "/" + path);
		} catch (KurentoException e) {
			assertThat(e.getMessage(), containsString("Timeout"));
		}

		Thread.sleep(2000);

		context.close();
	}

	private void testClientConnection(String url) throws IOException {

		JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(url);

		client.connectIfNecessary();

		assertThat(client.getWebSocketSession(), is(notNullValue()));

		client.close();
	}

	@Test
	public void configureWsGrizzlyPortTest() throws IOException,
			InterruptedException {

		String port = "5555";
		String path = "pathtest";

		System.setProperty(KurentoControlServerApp.WEBSOCKET_PORT_PROPERTY,
				port);
		System.setProperty(KurentoControlServerApp.WEBSOCKET_PATH_PROPERTY,
				path);

		ConfigurableApplicationContext context = KurentoControlServerApp
				.start();

		WebSocketTestClient.testConnection("ws://127.0.0.1:" + port + "/"
				+ path);

		try {
			WebSocketTestClient.testConnection("ws://127.0.0.1:" + (port + 2)
					+ "/" + path);
		} catch (RuntimeException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(DeploymentException.class)));
			assertThat(cause.getMessage(),
					containsString("Handshake response not received"));
		}

		Thread.sleep(2000);

		context.close();
	}

	@Test
	public void secureWsTest() throws IOException, InterruptedException,
			URISyntaxException {

		String port = "5555";
		String securePort = "9999";
		String path = "pathtest";

		System.setProperty(KurentoControlServerApp.WEBSOCKET_PORT_PROPERTY,
				port);

		System.setProperty(
				KurentoControlServerApp.WEBSOCKET_SECURE_PORT_PROPERTY,
				securePort);

		System.setProperty(KurentoControlServerApp.WEBSOCKET_PATH_PROPERTY,
				path);

		// Keystore generated with command:
		// keytool -genkey -alias tomcat -storetype PKCS12 -keystore keystore

		URI keystoreUri = this.getClass().getResource("/keystore").toURI();

		if (!keystoreUri.getScheme().equals("file")) {
			log.warn("This test can only be executed outside a jar file");
		}

		String keystorePath = Paths.get(keystoreUri).toAbsolutePath()
				.toString();

		System.setProperty(KurentoControlServerApp.KEYSTORE_FILE_PROPERTY,
				keystorePath);

		String keystorePass = "tomcat";

		System.setProperty(KurentoControlServerApp.KEYSTORE_PASS_PROPERTY,
				keystorePass);

		ConfigurableApplicationContext context = KurentoControlServerApp
				.start();

		WebSocketTestClient.testConnection("wss://localhost:" + securePort
				+ "/" + path, keystorePath, keystorePass);

		Thread.sleep(2000);

		context.close();
	}
}
