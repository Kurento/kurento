package com.kurento.kmf.thrift.test;

import java.io.IOException;

import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcServerThrift;
import com.kurento.kmf.thrift.test.util.EchoJsonRpcHandler;

public class JsonRpcConnectorClientServerTest {

	static class Params {
		String param1;
		String param2;
	}

	private static Logger log = LoggerFactory
			.getLogger(JsonRpcConnectorClientServerTest.class);

	@Test
	public void test() throws TException, IOException {

		log.info("Starting server");
		JsonRpcServerThrift server = new JsonRpcServerThrift(
				new EchoJsonRpcHandler(), "127.0.0.1", 19292);

		server.start();
		log.info("Server started");

		log.info("Starting client");

		JsonRpcClient client = new JsonRpcClientThrift("127.0.0.1", 19292,
				"127.0.0.1", 7979);

		Params params = new Params();
		params.param1 = "Value1";
		params.param2 = "Value2";

		Params result = client.sendRequest("echo", params, Params.class);

		log.info("Response:" + result);

		Assert.assertEquals(params.param1, result.param1);
		Assert.assertEquals(params.param2, result.param2);

		client.close();

		log.info("Client finished");

		server.destroy();

		log.info("Server finished");

	}
}
