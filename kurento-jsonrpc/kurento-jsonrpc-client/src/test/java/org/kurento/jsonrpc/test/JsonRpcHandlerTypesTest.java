package org.kurento.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.message.Request;

public class JsonRpcHandlerTypesTest {

	static class Params {
		String param1;
		String param2;
	}

	static class JsonRpcHandlerParams extends DefaultJsonRpcHandler<Params> {
		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {
		}
	}

	static interface FakeInterface<E> {
	}

	static class JsonRpcHandlerParamsMulti extends
			DefaultJsonRpcHandler<Params> implements FakeInterface<Object> {
		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {
		}
	}

	static class JsonRpcHandlerGrandson extends JsonRpcHandlerParams {
		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {
		}
	}

	static class JsonRpcHandlerDefault extends DefaultJsonRpcHandler<Params> {
		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {
		}
	}

	@Test
	public void getParamsTypeTest() {

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new JsonRpcHandlerParams().getHandlerType()));

		Assert.assertEquals(Params.class,
				JsonRpcHandlerManager
						.getParamsType(new JsonRpcHandlerParamsMulti()
								.getHandlerType()));

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new JsonRpcHandlerGrandson().getHandlerType()));

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new JsonRpcHandlerDefault().getHandlerType()));

	}
}
