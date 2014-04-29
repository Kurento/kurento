package com.kurento.kmf.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcHandlerManager;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.server.PerSessionJsonRpcHandler;

public class JsonRpcHandlerTypesTest {

	static class Params {
		String param1;
		String param2;
	}

	static class JsonRpcHandlerDefault extends DefaultJsonRpcHandler<Params> {
		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {
		}
	}

	// TODO where to make this test
	@Test
	public void getParamsTypeTest() {

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new PerSessionJsonRpcHandler<Params>(null,
						JsonRpcHandlerDefault.class).getHandlerType()));

	}
}
