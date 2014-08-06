package org.kurento.kmf.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kurento.kmf.jsonrpcconnector.JsonUtils;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;
import org.kurento.kmf.jsonrpcconnector.internal.message.Response;

public class GenericMessageTest {

	static class Params {
		String param1;
		String param2;
		Data data;
	}

	static class Data {
		String data1;
		String data2;
	}

	private static final Logger log = LoggerFactory
			.getLogger(GenericMessageTest.class);

	@Test
	public void requestTest() {

		Params params = new Params();
		params.param1 = "Value1";
		params.param2 = "Value2";
		params.data = new Data();
		params.data.data1 = "XX";
		params.data.data2 = "YY";

		Request<Params> request = new Request<Params>(1, "method", params);

		String requestJson = JsonUtils.toJsonRequest(request);

		log.info(requestJson);

		Request<Params> newRequest = JsonUtils.fromJsonRequest(requestJson,
				Params.class);

		Assert.assertEquals(params.param1, newRequest.getParams().param1);
		Assert.assertEquals(params.param2, newRequest.getParams().param2);
		Assert.assertEquals(params.data.data1,
				newRequest.getParams().data.data1);
		Assert.assertEquals(params.data.data2,
				newRequest.getParams().data.data2);

	}

	@Test
	public void responseTest() {

		Data data = new Data();
		data.data1 = "Value1";
		data.data2 = "Value2";

		Response<Data> request = new Response<Data>(1, data);

		String requestJson = JsonUtils.toJsonResponse(request);

		log.info(requestJson);

		Response<Data> newRequest = JsonUtils.fromJsonResponse(requestJson,
				Data.class);

		Assert.assertEquals(data.data1, newRequest.getResult().data1);
		Assert.assertEquals(data.data2, newRequest.getResult().data2);

	}

}
