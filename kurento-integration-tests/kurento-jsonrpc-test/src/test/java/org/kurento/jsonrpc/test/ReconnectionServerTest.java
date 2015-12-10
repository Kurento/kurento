package org.kurento.jsonrpc.test;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

public class ReconnectionServerTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(ReconnectionServerTest.class);

	private static final long TIMEOUT = 2;

	private static Semaphore s = new Semaphore(0);

	public static class Handler extends DefaultJsonRpcHandler<String> {

		private Session session;
		private ScheduledExecutorService executor = Executors
				.newScheduledThreadPool(1);

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			log.info("Receive request in server: " + request);

			if (session == null) {
				session = transaction.getSession();
			}

			if (session.isNew()) {
				transaction.sendResponse("new");
			} else {
				transaction.sendResponse("old");
			}

			log.info("Response sent from server");

			executor.schedule(new Runnable() {
				@Override
				public void run() {
					try {
						log.info("Request send from server");
						JsonElement result = session.sendRequest("hello");
						log.info("Response received in server");
						log.info("Result: " + result);
						s.release();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, 500, TimeUnit.MILLISECONDS);
		}

		@Override
		public void afterConnectionEstablished(Session session)
				throws Exception {
			session.setReconnectionTimeout(5000);
		}
	}

	@Test
	public void test() throws IOException, InterruptedException {

		JsonRpcClient client = new JsonRpcClientWebSocket("ws://localhost:"
				+ getPort() + "/reconnection2");
		client.setServerRequestHandler(new DefaultJsonRpcHandler<JsonElement>() {

			@Override
			public void handleRequest(Transaction transaction,
					Request<JsonElement> request) throws Exception {

				log.info("Receive request in client: " + request);
				transaction.sendResponse("world");
				log.info("Response sent from client");
			}
		});

		Assert.assertEquals("new",
				client.sendRequest("sessiontest", String.class));

		waitForServer();

		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));

		waitForServer();

		log.info("SessionId: " + client.getSession().getSessionId());

		JsonRpcClientWebSocket webSocketClient = (JsonRpcClientWebSocket) client;
		webSocketClient.closeNativeSession();

		Thread.sleep(100);

		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));

		waitForServer();

		log.info("Acquired");

		client.close();

	}

	private void waitForServer() throws InterruptedException {
		if (!s.tryAcquire(TIMEOUT, TimeUnit.SECONDS)) {
			throw new RuntimeException(
					"Timeout waiting for request from server");
		}
	}

}
