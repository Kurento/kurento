package org.kurento.jsonrpc.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.JsonRpcConnectorBaseTest;

public class BidirectionalMultiTest extends JsonRpcConnectorBaseTest {

	public static class Handler extends DefaultJsonRpcHandler<Integer> {

		private static Logger log = LoggerFactory.getLogger(Handler.class);

		@Override
		public void handleRequest(Transaction transaction,
				Request<Integer> request) throws Exception {

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

					for(int i=0; i<5;i++){
						Object response = session.sendRequest("method", params);
						session.sendRequest("method", response);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (InterruptedException e) {
			}
		}

	}

	private static final Logger log = LoggerFactory
			.getLogger(BidirectionalMultiTest.class);

	@Test
	public void test() throws IOException, InterruptedException {

		log.info("Client started");

		JsonRpcClient client = createJsonRpcClient("/BidirectionalMultiTest");

		client.setServerRequestHandler(new DefaultJsonRpcHandler<Integer>() {

			@Override
			public void handleRequest(Transaction transaction,
					Request<Integer> request) throws Exception {

				log.info("Reverse request: " + request);
				transaction.sendResponse(request.getParams()+1);
			}
		});

		for (int i = 0; i < 60; i++) {
			client.sendRequest("echo", i, Integer.class);
		}

		client.close();

		log.info("Client finished");
	}

}
