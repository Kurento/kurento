package org.kurento.kmf.rabbitmq.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import org.kurento.kmf.rabbitmq.client.JsonRpcClientRabbitMq;
import org.kurento.kmf.rabbitmq.manager.No2xxOKStatusResponseException;
import org.kurento.kmf.rabbitmq.manager.RabbitManager;

public class RabbitManagerTest {

	private static final String PIPELINE_CREATION = "pipeline_creation";

	@Test
	public void test() throws IOException {

		JsonRpcClientRabbitMq client = new JsonRpcClientRabbitMq();

		RabbitManager manager = new RabbitManager();
		System.out.println(manager.getQueueInfo(PIPELINE_CREATION));

		client.close();

		System.out.println(manager.getQueueInfo(PIPELINE_CREATION));

		manager.deleteQueue(PIPELINE_CREATION);

		try {
			System.out.println(manager.getQueueInfo(PIPELINE_CREATION));
		} catch (No2xxOKStatusResponseException e) {

			int statusCode = e.getResponse().getStatusLine().getStatusCode();
			Assert.assertEquals("StatusCode", 404, statusCode);

			System.out.println("Status code: "
					+ e.getResponse().getStatusLine().getStatusCode());
		}
	}

}
