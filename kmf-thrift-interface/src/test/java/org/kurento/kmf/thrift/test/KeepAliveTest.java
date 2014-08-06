package org.kurento.kmf.thrift.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import org.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.kmf.jsonrpcconnector.KeepAliveManager;
import org.kurento.kmf.jsonrpcconnector.Transaction;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;
import org.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import org.kurento.kmf.thrift.jsonrpcconnector.JsonRpcServerThrift;

public class KeepAliveTest {

	private static final int NUM_KEEP_ALIVES = 5;

	private static Logger log = LoggerFactory.getLogger(KeepAliveTest.class);

	private CountDownLatch latch = new CountDownLatch(NUM_KEEP_ALIVES);

	public class EchoJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

		@Override
		public void handleRequest(Transaction transaction,
				Request<JsonObject> request) throws Exception {

			if ("keepAlive".equals(request.getMethod())) {
				log.info("keepAlive");
				latch.countDown();
			}

			transaction.sendResponse(null);
		}
	}

	@Test
	public void test() throws TException, IOException, InterruptedException {

		System.setProperty(KeepAliveManager.KEEP_ALIVE_INTERVAL_TIME_PROPERTY,
				"1000");

		log.info("Starting server");
		JsonRpcServerThrift server = new JsonRpcServerThrift(
				new EchoJsonRpcHandler(), "127.0.0.1", 19292);

		server.start();
		log.info("Server started");

		long initTime = System.nanoTime();

		log.info("Starting client");
		JsonRpcClient client = new JsonRpcClientThrift("127.0.0.1", 19292,
				"127.0.0.1", 7979);

		if (!latch.await(15, TimeUnit.SECONDS)) {
			Assert.fail("Timeout of 15s waiting for keepAlives");
		} else {
			long duration = ((System.nanoTime() - initTime) / 1000000);

			Assert.assertTrue(
					"Waiting time should be greather than estimated keepAlive time ("
							+ duration + " > " + (NUM_KEEP_ALIVES * 1000) + ")",
					duration > NUM_KEEP_ALIVES * 1000);

			Assert.assertTrue(
					"Waiting time should be less than 1 keepAlive more than estimated keepAlive time ("
							+ duration
							+ " < "
							+ ((NUM_KEEP_ALIVES + 1) * 1000)
							+ ")", duration < (NUM_KEEP_ALIVES + 1) * 1000);

		}

		client.close();

		log.info("Client finished");

		server.destroy();

		log.info("Server finished");

	}
}
