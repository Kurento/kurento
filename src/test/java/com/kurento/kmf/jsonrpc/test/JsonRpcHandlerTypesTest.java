package com.kurento.kmf.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcHandlerManager;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.server.PerSessionJsonRpcHandler;

public class JsonRpcHandlerTypesTest {

	static class Params {
		String param1;
		String param2;
	}

	static class JsonRpcHandlerParams implements JsonRpcHandler<Params> {
		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {
		}

		@Override
		public void afterConnectionEstablished(Session session)
				throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterConnectionClosed(Session session, String status)
				throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleTransportError(Session session, Throwable exception)
				throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleUncaughtException(Session session, Exception exception) {
			// TODO Auto-generated method stub

		}
	}

	static interface FakeInterface<E> {
	}

	static class JsonRpcHandlerParamsMulti implements JsonRpcHandler<Params>,
			FakeInterface<Object> {
		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {
		}

		@Override
		public void afterConnectionEstablished(Session session)
				throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterConnectionClosed(Session session, String status)
				throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleTransportError(Session session, Throwable exception)
				throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleUncaughtException(Session session, Exception exception) {
			// TODO Auto-generated method stub

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

		Assert.assertEquals(Params.class,
				JsonRpcHandlerManager.getParamsType(new JsonRpcHandlerParams()));

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new JsonRpcHandlerParamsMulti()));

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new JsonRpcHandlerGrandson()));

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new JsonRpcHandlerDefault()));

		Assert.assertEquals(Params.class, JsonRpcHandlerManager
				.getParamsType(new PerSessionJsonRpcHandler<Params>(null,
						JsonRpcHandlerDefault.class)));

	}
}
