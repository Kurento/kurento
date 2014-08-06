package org.kurento.jsonrpcconnector.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.jsonrpcconnector.Session;
import org.kurento.jsonrpcconnector.Transaction;
import org.kurento.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.jsonrpcconnector.internal.message.Request;
import org.kurento.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class ServerEventsTest extends JsonRpcConnectorBaseTest {

	private static CountDownLatch afterConnectionEstablishedLatch = new CountDownLatch(
			1);
	private static CountDownLatch afterConnectionClosedLatch = new CountDownLatch(
			1);
	private static CountDownLatch requestLatch = new CountDownLatch(1);

	public static class Handler extends DefaultJsonRpcHandler<String> {

		private static Logger log = LoggerFactory.getLogger(Handler.class);

		@Override
		public void afterConnectionEstablished(Session session)
				throws Exception {

			session.setReconnectionTimeout(500);

			log.info("Connection established with sessionId: "
					+ session.getSessionId());
			afterConnectionEstablishedLatch.countDown();
		}

		@Override
		public void handleRequest(Transaction transaction,
				Request<String> request) throws Exception {

			log.info("Request: " + request);
			transaction.sendResponse(request.getParams());
			requestLatch.countDown();
		}

		@Override
		public void afterConnectionClosed(Session session, String status)
				throws Exception {

			log.info("Connection closed: " + status);
			afterConnectionClosedLatch.countDown();
		}
	}

	private static final Logger log = LoggerFactory
			.getLogger(ServerEventsTest.class);

	private static final long TIMEOUT = 5000;

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient("/serverevents");

		String result = client.sendRequest("echo", "params", String.class);

		Assert.assertTrue(
				"The method 'afterConnectionEstablished' is not invoked",
				afterConnectionEstablishedLatch.await(TIMEOUT,
						TimeUnit.MILLISECONDS));

		log.info("Response:" + result);

		Assert.assertEquals("params", result);

		Assert.assertTrue("The method 'handleRequest' is not invoked",
				requestLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

		client.close();

		Assert.assertTrue("The method 'afterConnectionClosed' is not invoked",
				afterConnectionClosedLatch
						.await(TIMEOUT, TimeUnit.MILLISECONDS));

		log.info("Client finished");

	}

}
