package org.kurento.jsonrpc.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

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
