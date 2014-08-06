package org.kurento.jsonrpcconnector.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.jsonrpcconnector.Transaction;
import org.kurento.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.jsonrpcconnector.internal.message.Request;
import org.kurento.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class NewSessionTest extends JsonRpcConnectorBaseTest {

	public static class Handler extends DefaultJsonRpcHandler<String> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			if (transaction.getSession().isNew()) {
				transaction.sendResponse("new");
			} else {
				transaction.sendResponse("old");
			}
		}
	}

	@Test
	public void test() throws IOException, InterruptedException {

		JsonRpcClient client = createJsonRpcClient("/new_session_handler");

		Assert.assertEquals("new",
				client.sendRequest("sessiontest", String.class));
		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));
		Assert.assertEquals("old",
				client.sendRequest("sessiontest", String.class));

		client.close();

	}

}
