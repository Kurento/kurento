package com.kurento.kmf.thrift.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.thrift.test.util.JsonRpcConnectorBaseTest;

//TODO: This test is a copy of similar test in jsonrpcconnector-test project.
//It is necessary to make all jsonrpcconnector-test tests work with Thrift 
//transport
public class TwiceAsyncClientEchoTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(TwiceAsyncClientEchoTest.class);

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		final JsonRpcClient client = createJsonRpcClient();

		final JsonObject params = new JsonObject();
		params.addProperty("param1", "Value1");
		params.addProperty("param2", "Value2");

		final CountDownLatch secondResponseRecLatch = new CountDownLatch(1);

		client.sendRequest("echo", params, new Continuation<JsonElement>() {

			@Override
			public void onSuccess(JsonElement result) {
				log.info("Response:" + result);

				JsonObject jsonResult = (JsonObject) result;

				Assert.assertEquals(jsonResult.get("param1").getAsString(),
						"Value1");
				Assert.assertEquals(jsonResult.get("param2").getAsString(),
						"Value2");

				final JsonObject params2 = new JsonObject();
				params2.addProperty("param3", "Value3");
				params2.addProperty("param4", "Value4");

				client.sendRequest("echo", params2,
						new Continuation<JsonElement>() {

							@Override
							public void onSuccess(JsonElement result) {
								log.info("Response:" + result);

								JsonObject jsonResult2 = (JsonObject) result;

								Assert.assertEquals(jsonResult2.get("param3")
										.getAsString(), "Value3");
								Assert.assertEquals(jsonResult2.get("param4")
										.getAsString(), "Value4");

								secondResponseRecLatch.countDown();
							}

							@Override
							public void onError(Throwable cause) {
								cause.printStackTrace();
							}
						});
			}

			@Override
			public void onError(Throwable cause) {
				cause.printStackTrace();
			}
		});

		Assert.assertTrue("Response not received in 5s",
				secondResponseRecLatch.await(5, TimeUnit.SECONDS));

		client.close();

		log.info("Client finished");
	}
}
