package org.kurento.rabbitmq.test;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.rabbitmq.RabbitMqManager;
import org.kurento.rabbitmq.client.JsonRpcClientRabbitMq;
import org.kurento.rabbitmq.manager.No2xxOKStatusResponseException;
import org.kurento.rabbitmq.manager.RabbitManager;
import org.kurento.rabbitmq.server.JsonRpcServerRabbitMq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

@Ignore
public class RetryTest {

	private static final int TIMEOUT_RETRY_TIME = 400;
	private static Logger log = LoggerFactory.getLogger(RetryTest.class);
	private KurentoClient kurento;
	private JsonRpcServerRabbitMq server;
	private JsonRpcClientRabbitMq client;

	@Before
	public void init() throws IOException {

		RabbitManager manager = new RabbitManager();

		try {
			manager.deleteQueue(RabbitMqManager.PIPELINE_CREATION_QUEUE);
			log.info("Queue '{}' deleted",
					RabbitMqManager.PIPELINE_CREATION_QUEUE);
		} catch (No2xxOKStatusResponseException e) {
			log.info("Queue '{}' doesn't exist",
					RabbitMqManager.PIPELINE_CREATION_QUEUE);
		}

		System.setProperty(RabbitMqManager.RETRY_TIMEOUT_PROPERTY,
				Integer.toString(TIMEOUT_RETRY_TIME));
		System.setProperty(RabbitMqManager.NUM_RETRIES_PROPERTY, "3");

		log.info("Starting client");
		client = new JsonRpcClientRabbitMq();

		kurento = KurentoClient.createFromJsonRpcClient(client);
	}

	@After
	public void teardown() throws IOException {
		client.close();
		log.info("Client finished");

		server.destroy();
		log.info("Server finished");
	}

	@Test
	public void simpleRetryTest() throws IOException, InterruptedException {

		server = new JsonRpcServerRabbitMq(
				new DefaultJsonRpcHandler<JsonObject>() {

					private int numRequests = 0;

					@Override
					public void handleRequest(Transaction transaction,
							Request<JsonObject> request) throws Exception {

						log.debug("Received request: " + request);

						numRequests++;
						if (numRequests == 1) {
							Thread.sleep(500000);
						}

						transaction.sendResponse("ObjectId");
					}
				});
		server.start();

		long initTime = System.nanoTime();

		kurento.createMediaPipeline();

		double duration = (System.nanoTime() - initTime) / (double) 1000000;

		log.info("Duration: " + duration);

		Assert.assertTrue("Duration must be grather than Timeout retry time",
				duration > TIMEOUT_RETRY_TIME);

	}

	@Test
	public void replyBeforeNextRetry() throws IOException, InterruptedException {

		server = new JsonRpcServerRabbitMq(
				new DefaultJsonRpcHandler<JsonObject>() {

					@Override
					public void handleRequest(Transaction transaction,
							Request<JsonObject> request) throws Exception {

						log.debug("Received request: " + request);

						Thread.sleep(TIMEOUT_RETRY_TIME + 50);

						transaction.sendResponse("ObjectId");
					}
				});
		server.start();

		long initTime = System.nanoTime();

		kurento.createMediaPipeline();

		double duration = (System.nanoTime() - initTime) / (double) 1000000;

		log.info("Duration: " + duration);

		Assert.assertTrue("Duration must be grather than Timeout retry time",
				duration > TIMEOUT_RETRY_TIME);

	}

}
