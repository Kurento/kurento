/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.rabbitmq;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.commons.testing.SystemTests;

/**
 * 
 * <strong>Description</strong>: Several clients are using several media server
 * by means of RabbitMQ transport. NOTE: This test is ignored because CI server
 * is not able to open several media server so far.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Browser ends before default timeout</li>
 * <li>The player should fire 'EndOfStreamEvent'</li>
 * </ul>
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
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
