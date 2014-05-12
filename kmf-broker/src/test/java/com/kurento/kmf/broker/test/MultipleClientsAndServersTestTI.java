package com.kurento.kmf.broker.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.broker.test.mock.ClientApp;
import com.kurento.kmf.broker.test.mock.MediaServer;

public class MultipleClientsAndServersTestTI {

	private static Logger LOG = LoggerFactory
			.getLogger(MultipleClientsAndServersTestTI.class);

	@Test
	public void test() throws InterruptedException {

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

		LOG.debug("Started -------------------------------------");

		for (ClientApp client : clients) {
			client.await();
		}

	}
}
