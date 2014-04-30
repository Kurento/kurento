package com.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientWebSocket;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcErrorException;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcConstants;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

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
			session.setReconnectionTimeout(500);
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

			WebSocketSession session = webSocketClient.getWebSocketSession();
			session.close();

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
			WebSocketSession session = webSocketClient.getWebSocketSession();
			session.close();

			Thread.sleep(1000);

			try {
				client.sendRequest("sessiontest", String.class);
				Assert.fail("The reconnection shoudn't be succesful because timeout");
			} catch (JsonRpcErrorException e) {
				Assert.assertEquals(JsonRpcConstants.RECONNECTION_ERROR,
						e.getMessage());
			}
		}

		client.close();

	}

}
