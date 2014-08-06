package org.kurento.kmf.jsonrpcconnector.test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.kmf.jsonrpcconnector.KeepAliveManager;
import org.kurento.kmf.jsonrpcconnector.Transaction;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClientLocal;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;

public class KeepAliveManagerTest {

	private static final int NUM_KEEP_ALIVES = 3;
	private static final int TIMEOUT_TIME = 5000;
	private static final int KEEPALIVE_INTERVAL_TIME = 500;
	private static final long MARGIN_TIME = 50;

	private static Logger log = LoggerFactory.getLogger(KeepAliveManagerTest.class);

	static class JsonRpcClientKeepAliveController {

		private BlockingQueue<Object> events = new LinkedBlockingQueue<Object>();

		private JsonRpcClient client = new JsonRpcClientLocal(
				new DefaultJsonRpcHandler<Object>() {
					@Override
					public void handleRequest(Transaction transaction,
							Request<Object> request) throws Exception {
						request(request);
					}
				});

		protected synchronized void request(Request<Object> request) {
			log.info("Request:" + request);
			events.add(new Object());
		}

		public JsonRpcClient getClient() {
			return client;
		}

		public long waitForEvents(int numEvents) throws InterruptedException {

			long startTime = System.nanoTime();

			for (int i = 0; i < numEvents; i++) {
				if (events.poll(TIMEOUT_TIME, TimeUnit.MILLISECONDS) == null) {
					Assert.fail("Timeout waiting for keepAlive requests");
				}
			}

			long finalTime = System.nanoTime();

			return (long) ((finalTime - startTime) / 1000000);
		}

		public synchronized int clearEvents() {
			int numEvents = events.size();
			events.clear();
			return numEvents;
		}
	}

	@Test
	public void oneSessionTest() throws InterruptedException {

		JsonRpcClientKeepAliveController controller = new JsonRpcClientKeepAliveController();

		KeepAliveManager keepAliveManager = new KeepAliveManager(
				controller.getClient(), KEEPALIVE_INTERVAL_TIME,
				KeepAliveManager.Mode.PER_CLIENT);

		keepAliveManager.start();

		long duration = controller.waitForEvents(3);

		Assert.assertTrue(duration + MARGIN_TIME > KEEPALIVE_INTERVAL_TIME
				* NUM_KEEP_ALIVES);

		keepAliveManager.stop();
	}

	@Test
	public void oneMediaPipelineTest() throws InterruptedException {

		JsonRpcClientKeepAliveController controller = new JsonRpcClientKeepAliveController();

		KeepAliveManager keepAliveManager = new KeepAliveManager(
				controller.getClient(), KEEPALIVE_INTERVAL_TIME,
				KeepAliveManager.Mode.PER_ID_AS_SESSION);

		keepAliveManager.start();

		Thread.sleep(KEEPALIVE_INTERVAL_TIME * 2);

		Assert.assertTrue(
				"KeepAliveManager in pipeline mode without pipelines shouldn't send any keepAlive event",
				controller.clearEvents() == 0);

		keepAliveManager.addId("XXXXX");

		Thread.sleep(KEEPALIVE_INTERVAL_TIME * 3);

		Assert.assertTrue("keepAlive events should be at least 2",
				controller.clearEvents() >= 2);

		keepAliveManager.stop();
	}

	@Test
	public void addRemovePipelinesTest() throws InterruptedException {

		JsonRpcClientKeepAliveController controller = new JsonRpcClientKeepAliveController();

		KeepAliveManager keepAliveManager = new KeepAliveManager(
				controller.getClient(), KEEPALIVE_INTERVAL_TIME,
				KeepAliveManager.Mode.PER_ID_AS_SESSION);

		keepAliveManager.start();

		String mediaPipelineIdA = "XXXXX";
		String mediaPipelineIdB = "YYYYY";

		keepAliveManager.addId(mediaPipelineIdA);
		keepAliveManager.addId(mediaPipelineIdB);

		Thread.sleep(KEEPALIVE_INTERVAL_TIME * 3);

		Assert.assertTrue("keepAlive events should be at least 2 x 2 = 4",
				controller.clearEvents() >= 4);

		keepAliveManager.removeId(mediaPipelineIdA);
		keepAliveManager.removeId(mediaPipelineIdB);

		Thread.sleep(KEEPALIVE_INTERVAL_TIME);
		log.info("Removed media pipelines and wait a keepAliveIntervalTime");

		controller.clearEvents();
		log.info("Cleared events after removing media pipelines");

		Thread.sleep(KEEPALIVE_INTERVAL_TIME * 3);

		Assert.assertTrue(
				"keepAliveManager without pipelines shouldn't send keepAlives",
				controller.clearEvents() == 0);

		keepAliveManager.stop();
	}
}
