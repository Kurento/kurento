package org.kurento.jsonrpc.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

public class ErrorServerTest extends JsonRpcConnectorBaseTest {

	public static class Handler extends DefaultJsonRpcHandler<String> {

		@Override
		public void handleRequest(final Transaction transaction,
				Request<String> request) throws Exception {

			String method = request.getMethod();

			if (method.equals("explicitError")) {

				transaction.sendError(-1, "Exception message", "Data");

			} else if (method.equals("asyncError")) {

				transaction.startAsync();

				// Poor man method scheduling
				new Thread() {
					@Override
					public void run() {
						try {
							Thread.sleep(1000);
							transaction.sendError(-1, "Exception message",
									"Data");
						} catch (Exception e) {
						}
					}
				}.start();

			} else if (method.equals("exceptionError")) {

				// 1, e.getMessage(), null
				throw new RuntimeException("Exception message");
			}
		}
	}

	// TODO this test has been disabled awaiting fixup
	@Ignore
	@Test
	public void test() throws IOException, InterruptedException {

		JsonRpcClient client = createJsonRpcClient("/error_handler");

		try {

			client.sendRequest("explicitError");

			Assert.fail("An exception should be thrown");

		} catch (JsonRpcErrorException e) {

			checkException(e, "Exception message", "Data");
		}

		try {

			client.sendRequest("asyncError");

			Assert.fail("An exception should be thrown");

		} catch (JsonRpcErrorException e) {

			checkException(e, "Exception message", "Data");
		}

		try {

			client.sendRequest("exceptionError");

			Assert.fail("An exception should be thrown");

		} catch (JsonRpcErrorException e) {

			checkException(e, "RuntimeException:Exception message.",
					"java.lang.RuntimeException:");
		}

		client.close();
	}

	private void checkException(JsonRpcErrorException e, String message,
			String data) {

		boolean expectedStartMessage = e.getMessage().startsWith(message);
		Assert.assertTrue("Exception should be an error starting with: '"
				+ message + "' but it is '" + e.getMessage() + "'",
				expectedStartMessage);

		boolean expectedStartData = e.getData().toString().startsWith(data);
		Assert.assertTrue(
				"Exception should have an error with data starting with: '"
						+ data + "' but it is '" + e.getData() + "'",
				expectedStartData);

		Assert.assertEquals(-1, e.getCode());
	}

}
