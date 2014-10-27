package org.kurento.jsonrpc.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectionTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(ReconnectionTest.class);

	public static class Handler extends DefaultJsonRpcHandler<String> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			Session session = transaction.getSession();

			if (session.isNew()) {
				transaction.sendResponse("new");
			} else {
				transaction.sendResponse("old");
			}
		}

		@Override
		public void afterConnectionEstablished(Session session)
				throws Exception {
			session.setReconnectionTimeout(5000);
		}
	}

	@Test
	public void test() throws IOException {

		JsonRpcClient client = createJsonRpcClient("/reconnection");

		if (client instanceof JsonRpcClientWebSocket) {

			Assert.assertEquals("new",
					client.sendRequest("sessiontest", String.class));
			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));
			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));

			log.info("SessionId: " + client.getSession().getSessionId());

			JsonRpcClientWebSocket webSocketClient = (JsonRpcClientWebSocket) client;
			webSocketClient.closeNativeSession();

			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));
			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));

			client.setSessionId(null);

			// With this we test if the transportId is used to recognize the
			// session

			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));
			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));

			log.info("SessionId: " + client.getSession().getSessionId());

		}

		client.close();

	}

	@Test
	public void timeoutReconnectionTest() throws IOException,
			InterruptedException {

		JsonRpcClient client = createJsonRpcClient("/reconnection");

		if (client instanceof JsonRpcClientWebSocket) {

			Assert.assertEquals("new",
					client.sendRequest("sessiontest", String.class));
			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));
			Assert.assertEquals("old",
					client.sendRequest("sessiontest", String.class));

			JsonRpcClientWebSocket webSocketClient = (JsonRpcClientWebSocket) client;
			webSocketClient.closeNativeSession();

			Thread.sleep(1000);

			try {
				Assert.assertEquals("old",
						client.sendRequest("sessiontest", String.class));
			} catch (JsonRpcErrorException e) {
				Assert.fail("Reconnection should be happend behind the covers");
			}
		}

		client.close();

	}

}
