package org.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import org.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.kmf.jsonrpcconnector.Transaction;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;
import org.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class NotificationTest extends JsonRpcConnectorBaseTest {

	private static CountDownLatch serverRequestLatch;

	public static class Handler extends DefaultJsonRpcHandler<Integer> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<Integer> request) throws Exception {

			if (!transaction.isNotification()) {
				throw new RuntimeException("Notification expected");
			}

			Thread.sleep(1000);

			transaction.getSession().sendNotification("response",
					request.getParams());
		}
	}

	@Test
	public void test() throws IOException, InterruptedException {

		serverRequestLatch = new CountDownLatch(3);

		JsonRpcClient client = createJsonRpcClient("/notification");

		client.setServerRequestHandler(new DefaultJsonRpcHandler<Integer>() {

			@Override
			public void handleRequest(Transaction transaction,
					Request<Integer> request) throws Exception {

				serverRequestLatch.countDown();
			}
		});

		client.sendNotification("echo", 1);
		client.sendNotification("echo", 2);
		client.sendNotification("echo", 3);

		Assert.assertTrue("The server has not invoked requests",
				serverRequestLatch.await(5000, TimeUnit.MILLISECONDS));

		client.close();

	}

}
