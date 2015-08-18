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
package org.kurento.test.internal;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kurento.test.grid.GridHandler;
import org.kurento.test.services.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility for killing all the processes of a user in a remote node
 * (for manual testing/debug purposes).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillAllProcesses {

	public static Logger log = LoggerFactory.getLogger(KillAllProcesses.class);

	public static void main(String[] args) throws InterruptedException {
		List<String> nodeList = GridHandler.getInstance().getNodeList();

		int nodeListSize = nodeList.size();
		log.debug("Node availables in the node list: {}", nodeListSize);
		ExecutorService executor = Executors.newFixedThreadPool(nodeListSize);
		final CountDownLatch latch = new CountDownLatch(nodeListSize);

		for (final String node : nodeList) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName(node);

					if (SshConnection.ping(node)) {
						SshConnection remoteHost = null;
						try {
							log.info("Openning connection to node {}", node);
							remoteHost = new SshConnection(node);
							remoteHost.start();
							remoteHost.execCommand("kill", "-9", "-1");
						} catch (Throwable e) {
							e.printStackTrace();
						} finally {
							if (remoteHost != null) {
								log.info("Closing connection to node {}", node);
								remoteHost.stop();
							}
						}
					} else {
						log.error("Node down {}", node);
					}

					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();
	}
}
