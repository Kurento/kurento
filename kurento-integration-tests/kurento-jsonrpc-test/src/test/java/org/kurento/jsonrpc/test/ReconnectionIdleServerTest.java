package org.kurento.jsonrpc.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.test.BasicEchoTest.Params;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectionIdleServerTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
	        .getLogger(ReconnectionIdleServerTest.class);

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
		        "ws://localhost:" + getPort() + "/jsonrpc");

		client.setIdleTimeout(5000);
		client.enableHeartbeat(4000);

		for (int i = 0; i < 5; i++) {

			Params params = new Params();
			params.param1 = "Value1";
			params.param2 = "Value2";

			Params result = client.sendRequest("echo", params, Params.class);

			log.info("Response:" + result);

			Assert.assertEquals(params.param1, result.param1);
			Assert.assertEquals(params.param2, result.param2);

			Thread.sleep(5000);

		}

		client.close();

		log.info("Client finished");

	}
}
