package org.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import org.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.kmf.jsonrpcconnector.Session;
import org.kurento.kmf.jsonrpcconnector.Transaction;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;
import org.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class CloseSessionTest extends JsonRpcConnectorBaseTest {

	public static class Handler extends DefaultJsonRpcHandler<String> {

		int counter = 0;

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			Session session = transaction.getSession();

			if (session.isNew()) {
				transaction.sendResponse("new");
			} else {
				transaction.sendResponse("old");
			}

			if (counter == 2) {
				session.close();
			}
			counter++;
		}
	}

	@Test
	public void test() throws IOException, InterruptedException {

		JsonRpcClient client = createJsonRpcClient("/close_session_handler");

		Assert.assertEquals("new",
				client.sendRequest("sessiontest", String.class));
		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));
		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));

		client = createJsonRpcClient("/close_session_handler");

		Assert.assertEquals("new",
				client.sendRequest("sessiontest", String.class));
		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));
		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));

		client.close();

	}

}
