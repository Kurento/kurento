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

public class ClientEventsTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(ClientEventsTest.class);

	private static final long TIMEOUT = 5000;

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient("/jsonrpcreverse");

		final CountDownLatch afterConnectionEstablishedLatch = new CountDownLatch(
				1);
		final CountDownLatch afterConnectionClosedLatch = new CountDownLatch(1);
		final CountDownLatch inverseRequestLatch = new CountDownLatch(2);
		final String[] inverseRequestParams = new String[1];

		client.setServerRequestHandler(new DefaultJsonRpcHandler<String>() {

			@Override
			public void afterConnectionEstablished(Session session)
					throws Exception {

				log.info("Connection established with sessionId: "
						+ session.getSessionId());
				afterConnectionEstablishedLatch.countDown();
			}

			@Override
			public void handleRequest(Transaction transaction,
					Request<String> request) throws Exception {

				log.info("Reverse request: " + request);

				transaction.sendResponse(request.getParams());
				inverseRequestParams[0] = request.getParams();

				inverseRequestLatch.countDown();
			}

			@Override
			public void afterConnectionClosed(Session session, String status)
					throws Exception {

				log.info("Connection closed: " + status);
				afterConnectionClosedLatch.countDown();
			}
		});

		String result = client.sendRequest("echo", "params", String.class);

		Assert.assertTrue(
				"The method 'afterConnectionEstablished' is not invoked",
				afterConnectionEstablishedLatch.await(TIMEOUT,
						TimeUnit.MILLISECONDS));

		log.info("Response:" + result);

		Assert.assertEquals("params", result);

		Assert.assertTrue("The method 'handleRequest' is not invoked",
				inverseRequestLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

		String newResult = inverseRequestParams[0];

		Assert.assertEquals("params", newResult);

		client.close();

		Assert.assertTrue("The method 'afterConnectionClosed' is not invoked",
				afterConnectionClosedLatch
						.await(TIMEOUT, TimeUnit.MILLISECONDS));

		log.info("Client finished");

	}

}
