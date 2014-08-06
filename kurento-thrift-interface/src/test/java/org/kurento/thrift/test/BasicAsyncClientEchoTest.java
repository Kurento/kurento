package org.kurento.thrift.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.thrift.test.util.JsonRpcConnectorBaseTest;

//TODO: This test is a copy of similar test in jsonrpcconnector-test project.
//It is necessary to make all jsonrpcconnector-test tests work with Thrift 
//transport
public class BasicAsyncClientEchoTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(BasicAsyncClientEchoTest.class);

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient();

		final JsonObject params = new JsonObject();
		params.addProperty("param1", "Value1");
		params.addProperty("param2", "Value2");

		final CountDownLatch responseRecLatch = new CountDownLatch(1);

		client.sendRequest("echo", params, new Continuation<JsonElement>() {

			@Override
			public void onSuccess(JsonElement result) {
				log.info("Response:" + result);

				Assert.assertEquals(params.get("param1").getAsString(),
						"Value1");
				Assert.assertEquals(params.get("param2").getAsString(),
						"Value2");

				responseRecLatch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				cause.printStackTrace();
			}
		});

		Assert.assertTrue("Response not received in 5s",
				responseRecLatch.await(5, TimeUnit.SECONDS));

		client.close();

		log.info("Client finished");
	}
}
