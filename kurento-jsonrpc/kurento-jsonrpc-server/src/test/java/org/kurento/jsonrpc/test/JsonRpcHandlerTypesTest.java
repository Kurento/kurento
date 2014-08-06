package org.kurento.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.internal.server.PerSessionJsonRpcHandler;
import org.kurento.jsonrpc.message.Request;

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
