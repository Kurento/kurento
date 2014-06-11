package com.kurento.kmf.test.rabbitmq;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.commons.tests.SystemTests;

@Category(SystemTests.class)
public class MultipleClientsAndServersTest {

	private static final Logger log = LoggerFactory
			.getLogger(MultipleClientsAndServersTest.class);

	@Ignore
	@Test
	public void testMultipleClientsAndServers() throws InterruptedException {

		int numMediaServers = 2;
		int numClientApps = 2;

		for (int i = 0; i < numMediaServers; i++) {
			new MediaServer(i).start();
		}

		List<ClientApp> clients = new ArrayList<ClientApp>();
		for (int i = 0; i < numClientApps; i++) {
			ClientApp client = new ClientApp("C" + i);
			client.start();
			clients.add(client);
		}

		log.debug("Multiple clients started");

		for (ClientApp client : clients) {
			client.await();
		}

	}
}
