package org.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class BasicEchoTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(BasicEchoTest.class);

	static class Params {
		String param1;
		String param2;
	}

	@Test
	public void test() throws IOException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient("/jsonrpc");

		Params params = new Params();
		params.param1 = "Value1";
		params.param2 = "Value2";

		Params result = client.sendRequest("echo", params, Params.class);

		log.info("Response:" + result);

		Assert.assertEquals(params.param1, result.param1);
		Assert.assertEquals(params.param2, result.param2);

		client.close();

		log.info("Client finished");

	}

}
