package org.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.kurento.kmf.jsonrpcconnector.client.Continuation;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class BasicAsyncClientEchoTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(BasicAsyncClientEchoTest.class);

	static class Params {
		String param1;
		String param2;
	}

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient("/jsonrpc");

		final JsonObject params = new JsonObject();
		params.addProperty("param1", "Value1");
		params.addProperty("param2", "Value2");

		CountDownLatch finishTestLatch = new CountDownLatch(1);

		client.sendRequest("echo", params, new Continuation<JsonElement>() {

			@Override
			public void onSuccess(JsonElement result) {
				log.info("Response:" + result);

				Assert.assertEquals(params.get("param1").getAsString(),
						"Value1");
				Assert.assertEquals(params.get("param2").getAsString(),
						"Value2");
			}

			@Override
			public void onError(Throwable cause) {
				cause.printStackTrace();
			}
		});

		finishTestLatch.await(5, TimeUnit.SECONDS);

		client.close();

		log.info("Client finished");
	}

}
