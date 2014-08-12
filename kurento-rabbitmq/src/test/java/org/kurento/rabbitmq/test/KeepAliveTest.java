package org.kurento.rabbitmq.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.factory.KurentoClient;
import org.kurento.client.factory.KurentoClientFactory;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.KeepAliveManager;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.rabbitmq.client.JsonRpcClientRabbitMq;
import org.kurento.rabbitmq.server.JsonRpcServerRabbitMq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class KeepAliveTest {

	private static final int NUM_KEEP_ALIVES = 5;

	private static Logger log = LoggerFactory.getLogger(KeepAliveTest.class);

	private CountDownLatch latch = new CountDownLatch(NUM_KEEP_ALIVES);

	public class EchoJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

		private int numObjects = 0;

		@Override
		public void handleRequest(Transaction transaction,
				Request<JsonObject> request) throws Exception {

			if ("keepAlive".equals(request.getMethod())) {
				log.info("keepAlive");
				latch.countDown();
				transaction.sendResponse(null);
				return;
			}

			if ("create".equals(request.getMethod())) {
				transaction.sendResponse("ObjectId_" + numObjects);
				numObjects++;
			}
		}
	}

	@Test
	public void test() throws TException, IOException, InterruptedException {

		System.setProperty(KeepAliveManager.KEEP_ALIVE_INTERVAL_TIME_PROPERTY,
				"1000");

		log.info("Starting server");
		JsonRpcServerRabbitMq server = new JsonRpcServerRabbitMq(
				new EchoJsonRpcHandler());

		server.start();
		log.info("Server started");

		long initTime = System.nanoTime();

		log.info("Starting client");
		JsonRpcClientRabbitMq client = new JsonRpcClientRabbitMq();

		KurentoClient kurento = KurentoClientFactory
				.createWithJsonRpcClient(client);

		kurento.createMediaPipeline();

		checkKeepAlives(initTime, NUM_KEEP_ALIVES * 1000,
				(NUM_KEEP_ALIVES + 1) * 1000);

		// There are two pipelines and NUM_KEEP_ALIVES are submited in the half
		// of the time
		initTime = System.nanoTime();
		latch = new CountDownLatch(NUM_KEEP_ALIVES);

		kurento.createMediaPipeline();

		checkKeepAlives(initTime, NUM_KEEP_ALIVES * 1000 / 2,
				(NUM_KEEP_ALIVES + 1) * 1000 / 2);

		client.close();

		log.info("Client finished");

		server.destroy();

		log.info("Server finished");

	}

	private void checkKeepAlives(long initTime, long minTime, long maxTime)
			throws InterruptedException {
		if (!latch.await(1500, TimeUnit.SECONDS)) {
			Assert.fail("Timeout of 15s waiting for keepAlives");
		} else {
			long duration = ((System.nanoTime() - initTime) / 1000000);

			Assert.assertTrue(
					"Waiting time should be greather than estimated keepAlive time ("
							+ duration + " > " + minTime + ")",
					duration > minTime);

			Assert.assertTrue(
					"Waiting time should be less than 1 keepAlive more than estimated keepAlive time ("
							+ duration + " < " + maxTime + ")",
					duration < maxTime);

		}
	}
}
