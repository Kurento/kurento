package org.kurento.jsonrpcconnector.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.jsonrpcconnector.Transaction;
import org.kurento.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.jsonrpcconnector.internal.message.Request;
import org.kurento.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class AsyncServerTest extends JsonRpcConnectorBaseTest {

	public static class Handler extends DefaultJsonRpcHandler<String> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			transaction.startAsync();

			// Poor man method scheduling
			new Thread() {
				public void run() {
					try {
						Thread.sleep(1000);
						transaction.sendResponse("AsyncHello");
					} catch (Exception e) {
					}
				}
			}.start();
		}
	}

	@Test
	public void test() throws IOException, InterruptedException {

		JsonRpcClient client = createJsonRpcClient("/async_handler");

		String response = client.sendRequest("count", "fakeparams",
				String.class);

		Assert.assertEquals("AsyncHello", response);

		client.close();

	}

}
