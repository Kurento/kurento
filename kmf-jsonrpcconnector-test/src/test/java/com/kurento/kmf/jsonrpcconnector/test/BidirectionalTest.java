package com.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

public class BidirectionalTest extends JsonRpcConnectorBaseTest {

	public static class Handler extends DefaultJsonRpcHandler<Object> {

		private static Logger log = LoggerFactory.getLogger(Handler.class);

		@Override
		public void handleRequest(Transaction transaction,
				Request<Object> request) throws Exception {

			log.info("Request id:" + request.getId());
			log.info("Request method:" + request.getMethod());
			log.info("Request params:" + request.getParams());

			transaction.sendResponse(request.getParams());

			final Session session = transaction.getSession();
			final Object params = request.getParams();

			new Thread() {
				public void run() {
					asyncReverseSend(session, params);
				}
			}.start();
		}

		public void asyncReverseSend(Session session, Object params) {

			try {

				Thread.sleep(1000);

				try {

					Object response = session.sendRequest("method", params);
					session.sendRequest("method", response);

				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (InterruptedException e) {
			}
		}

	}

	private static final Logger log = LoggerFactory
			.getLogger(BidirectionalTest.class);

	public static class Params {
		String param1;
		String param2;
	}

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient("/jsonrpcreverse");

		final CountDownLatch inverseRequestLatch = new CountDownLatch(2);
		final Params[] inverseRequestParams = new Params[1];

		client.setServerRequestHandler(new DefaultJsonRpcHandler<Params>() {

			@Override
			public void handleRequest(Transaction transaction,
					Request<Params> request) throws Exception {

				log.info("Reverse request: " + request);

				transaction.sendResponse(request.getParams());
				inverseRequestParams[0] = request.getParams();

				inverseRequestLatch.countDown();
			}
		});

		Params params = new Params();
		params.param1 = "Value1";
		params.param2 = "Value2";

		Params result = client.sendRequest("echo", params, Params.class);

		log.info("Response:" + result);

		Assert.assertEquals(params.param1, result.param1);
		Assert.assertEquals(params.param2, result.param2);

		inverseRequestLatch.await();

		Params newResult = inverseRequestParams[0];

		Assert.assertEquals(params.param1, newResult.param1);
		Assert.assertEquals(params.param2, newResult.param2);

		client.close();

		log.info("Client finished");

	}

}
