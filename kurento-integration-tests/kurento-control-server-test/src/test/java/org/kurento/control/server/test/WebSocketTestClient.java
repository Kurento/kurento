package org.kurento.control.server.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.grizzly.GrizzlyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class WebSocketTestClient {

	private static Logger log = LoggerFactory
			.getLogger(WebSocketTestClient.class);

	private static CountDownLatch latch;

	@OnOpen
	public void onOpen(final Session session) throws InterruptedException,
			IOException {

		log.info("Connected ... " + session.getId());

		new Thread() {
			public void run() {
				try {
					Thread.sleep(5000);
					session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, ""));
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

	}

	@OnMessage
	public void onMessage(String message, Session session) {
		log.info("Message received: " + message);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		log.info(String.format("Session close %s because of %s",
				session.getId(), closeReason));
		latch.countDown();
	}

	public static void testConnection(String url) {
		testConnection(url, null, null);
	}

	public static void testConnection(String url, String keystoreFile,
			String keystorePass) {

		log.info("Connecting to: " + url);

		latch = new CountDownLatch(1);

		try {

			ClientManager client = ClientManager.createClient();

			if (keystoreFile != null) {

				System.getProperties().put("javax.net.debug", "all");
				System.getProperties().put(
						SSLContextConfigurator.KEY_STORE_FILE, keystoreFile);
				System.getProperties().put(
						SSLContextConfigurator.KEY_STORE_TYPE, "PKCS12");
				System.getProperties().put(
						SSLContextConfigurator.TRUST_STORE_FILE, keystoreFile);
				System.getProperties().put(
						SSLContextConfigurator.TRUST_STORE_TYPE, "PKCS12");
				System.getProperties()
						.put(SSLContextConfigurator.KEY_STORE_PASSWORD,
								keystorePass);
				System.getProperties().put(
						SSLContextConfigurator.TRUST_STORE_PASSWORD,
						keystorePass);

				SSLContextConfigurator defaultConfig = new SSLContextConfigurator();
				defaultConfig.retrieve(System.getProperties());

				SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(
						defaultConfig, true, false, false);

				client.getProperties().put(
						GrizzlyEngine.SSL_ENGINE_CONFIGURATOR,
						sslEngineConfigurator);

			}

			client.connectToServer(WebSocketTestClient.class,
					ClientEndpointConfig.Builder.create().build(), new URI(url));

			latch.await();

		} catch (DeploymentException | URISyntaxException
				| InterruptedException e) {

			throw new RuntimeException(e);
		}
	}
}