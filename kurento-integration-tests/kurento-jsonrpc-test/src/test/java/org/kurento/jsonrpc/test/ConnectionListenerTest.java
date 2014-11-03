package org.kurento.jsonrpc.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListenerTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(ConnectionListenerTest.class);

	public static class Handler extends DefaultJsonRpcHandler<String> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			transaction.sendResponse("Hello");
		}
	}

	@Test
	public void connectionTimeoutTest() throws IOException,
			InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);

		JsonRpcClient client = new JsonRpcClientWebSocket(
				"ws://localhost:65000/connectionlistener",
				new JsonRpcWSConnectionListener() {

					@Override
					public void disconnected() {
					}

					@Override
					public void connected() {

					}

					@Override
					public void connectionTimeout() {
						latch.countDown();
					}
				});

		try {
			client.sendRequest("sessiontest", String.class);

			if (!latch.await(20, TimeUnit.SECONDS)) {
				fail("Any of KurentoException should be thrown or connectionTimeout() event method should be called");
			}

		} catch (KurentoException e) {
			assertThat(e.getMessage(),
					containsString("Exception connecting to"));
		}

		client.close();
	}

	@Test
	public void connectedTest() throws IOException, InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);

		JsonRpcClient client = new JsonRpcClientWebSocket("ws://localhost:"
				+ getPort() + "/connectionlistener",
				new JsonRpcWSConnectionListener() {

					@Override
					public void disconnected() {
					}

					@Override
					public void connected() {
						log.info("connected");
						latch.countDown();
					}

					@Override
					public void connectionTimeout() {
						// TODO Auto-generated method stub

					}
				});

		client.sendRequest("sessiontest", String.class);

		if (!latch.await(20, TimeUnit.SECONDS)) {
			fail("Event connected() not thrown in 20s");
		}

		client.close();
	}

	@Test
	public void clientDisconnectedTest() throws IOException,
			InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);

		JsonRpcClient client = new JsonRpcClientWebSocket("ws://localhost:"
				+ getPort() + "/connectionlistener",
				new JsonRpcWSConnectionListener() {

					@Override
					public void disconnected() {
						log.info("disconnected");
						latch.countDown();
					}

					@Override
					public void connected() {
					}

					@Override
					public void connectionTimeout() {
						// TODO Auto-generated method stub

					}
				});

		client.sendRequest("sessiontest", String.class);
		client.close();

		if (!latch.await(20, TimeUnit.SECONDS)) {
			fail("Event disconnected() not thrown in 20s");
		}

		client.close();
	}

	@Test
	public void communicationFailureDisconnectionTest() throws IOException,
			InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);

		JsonRpcClient client = new JsonRpcClientWebSocket("ws://localhost:"
				+ getPort() + "/connectionlistener",
				new JsonRpcWSConnectionListener() {

					@Override
					public void disconnected() {
						System.out.println("disconnected");
						latch.countDown();
					}

					@Override
					public void connected() {
					}

					@Override
					public void connectionTimeout() {
						// TODO Auto-generated method stub

					}
				});

		client.sendRequest("sessiontest", String.class);

		JsonRpcClientWebSocket webSocketClient = (JsonRpcClientWebSocket) client;
		webSocketClient.closeNativeSession();

		if (latch.await(20, TimeUnit.SECONDS)) {
			fail("Event disconnected() not should be thrown "
					+ "because reconnection should be succesful");
		}

		client.close();
	}
}
