package org.kurento.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

public class SessionIdMessageTest {

	static class Params {
		String param1;
	}

	static class Data {
		String data1;
	}

	private static final Logger log = LoggerFactory
			.getLogger(SessionIdMessageTest.class);

	@Test
	public void requestTest() {

		Params params = new Params();
		params.param1 = "Value1";

		Request<Params> request = new Request<Params>(1, "method", params);
		request.setSessionId("xxxxxxx");

		String requestJson = request.toString();
		Assert.assertEquals(
				"{\"id\":1,\"method\":\"method\",\"params\":{\"param1\":\"Value1\",\"sessionId\":\"xxxxxxx\"},\"jsonrpc\":\"2.0\"}",
				requestJson);

		log.info(requestJson);

		Request<Params> newRequest = JsonUtils.fromJsonRequest(requestJson,
				Params.class);

		Assert.assertEquals(params.param1, newRequest.getParams().param1);
		Assert.assertEquals(newRequest.getSessionId(), "xxxxxxx");

	}

	@Test
	public void noParamsRequestTest() {

		Request<Void> request = new Request<Void>(1, "method", null);
		request.setSessionId("xxxxxxx");

		String requestJson = request.toString();
		Assert.assertEquals(
				"{\"id\":1,\"method\":\"method\",\"jsonrpc\":\"2.0\",\"params\":{\"sessionId\":\"xxxxxxx\"}}",
				requestJson);

		log.info(requestJson);

		Request<Void> newRequest = JsonUtils.fromJsonRequest(requestJson,
				Void.class);

		// Assert.assertEquals(null, newRequest.getParams());
		Assert.assertEquals(newRequest.getSessionId(), "xxxxxxx");

	}

	@Test
	public void noResultResponseTest() {

		Response<Void> response = new Response<Void>(1);
		response.setSessionId("xxxxxxx");

		String responseJson = response.toString();
		JsonParser parser = new JsonParser();
		JsonObject expected = (JsonObject) parser
				.parse("{\"id\":1,\"result\":{\"sessionId\":\"xxxxxxx\"},\"jsonrpc\":\"2.0\"}");
		JsonObject result = (JsonObject) parser.parse(responseJson);
		Assert.assertEquals(expected, result);

		log.info(responseJson);

		Response<Void> newResponse = JsonUtils.fromJsonResponse(responseJson,
				Void.class);

		// Assert.assertEquals(null, newResponse.getResult());
		Assert.assertEquals(newResponse.getSessionId(), "xxxxxxx");

	}

	@Test
	public void responseTest() {

		Data data = new Data();
		data.data1 = "Value1";

		Response<Data> response = new Response<Data>(1, data);
		response.setSessionId("xxxxxxx");

		String responseJson = response.toString();
		Assert.assertEquals(
				"{\"id\":1,\"result\":{\"data1\":\"Value1\",\"sessionId\":\"xxxxxxx\"},\"jsonrpc\":\"2.0\"}",
				responseJson);

		log.info(responseJson);

		Response<Data> newResponse = JsonUtils.fromJsonResponse(responseJson,
				Data.class);

		Assert.assertEquals(data.data1, newResponse.getResult().data1);
		Assert.assertEquals(newResponse.getSessionId(), "xxxxxxx");
	}

}
