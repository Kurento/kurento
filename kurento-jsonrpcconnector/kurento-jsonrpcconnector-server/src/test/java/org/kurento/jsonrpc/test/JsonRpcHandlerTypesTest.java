package org.kurento.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.jsonrpcconnector.Transaction;
import org.kurento.jsonrpcconnector.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpcconnector.internal.message.Request;
import org.kurento.jsonrpcconnector.internal.server.PerSessionJsonRpcHandler;

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
