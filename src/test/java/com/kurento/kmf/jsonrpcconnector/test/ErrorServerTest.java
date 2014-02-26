package com.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcErrorException;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

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

	@Test
	public void test() throws IOException, InterruptedException {

		JsonRpcClient client = createJsonRpcClient("/error_handler");

		try {

			client.sendRequest("explicitError");

			Assert.fail("An exception should be thrown");

		} catch (JsonRpcErrorException e) {

			Assert.assertEquals("Exception message. Data: Data", e.getMessage());
			Assert.assertEquals("Data", e.getData());
			Assert.assertEquals(-1, e.getCode());
		}

		try {

			client.sendRequest("asyncError");

			Assert.fail("An exception should be thrown");

		} catch (JsonRpcErrorException e) {

			Assert.assertEquals("Exception message. Data: Data", e.getMessage());
			Assert.assertEquals("Data", e.getData());
			Assert.assertEquals(-1, e.getCode());
		}

		try {

			client.sendRequest("exceptionError");

			Assert.fail("An exception should be thrown");

		} catch (RuntimeException e) {

			String expected = "RuntimeException:Exception message. Data: java.lang.RuntimeException: Exception message";
			Assert.assertEquals(expected, e.getMessage().substring(0,expected.length()));
		}

		client.close();

	}

}
