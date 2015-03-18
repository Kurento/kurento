/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.grid;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.CLIENT_RATE_DEFAULT;
import static org.kurento.test.TestConfiguration.CLIENT_RATE_PROPERTY;
import static org.kurento.test.TestConfiguration.HOLD_TIME_DEFAULT;
import static org.kurento.test.TestConfiguration.HOLD_TIME_PROPERTY;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserRunner;
import org.kurento.test.monitor.SystemMonitorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for launch browsers in parallel.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.1
 */
public class ParallelBrowsers {

	public static Logger log = LoggerFactory.getLogger(ParallelBrowsers.class);

	private static int clientRate = getProperty(CLIENT_RATE_PROPERTY,
			CLIENT_RATE_DEFAULT);

	private static int holdTime = getProperty(HOLD_TIME_PROPERTY,
			HOLD_TIME_DEFAULT);

	public static void ramp(final Map<String, BrowserClient> browserClientMap,
			final SystemMonitorManager monitor,
			final BrowserRunner browserRunner) {
		ExecutorService internalExec = Executors
				.newFixedThreadPool(browserClientMap.size());
		CompletionService<Void> exec = new ExecutorCompletionService<>(
				internalExec);

		int numBrowser = 0;
		for (final String key : browserClientMap.keySet()) {
			final int numBrowserFinal = numBrowser;
			exec.submit(new Callable<Void>() {
				public Void call() throws Exception {
					try {
						Thread.currentThread().setName(key);
						Thread.sleep(clientRate * numBrowserFinal);
						log.debug("*** Starting node {} ***", key);
						if (monitor != null) {
							monitor.incrementNumClients();
						}
						BrowserClient browser = browserClientMap.get(key);
						browserRunner.run(browser);
					} finally {
						if (monitor != null) {
							monitor.decrementNumClients();
						}
						log.debug("--- Ending client {} ---", key);
					}
					return null;
				}
			});
			numBrowser++;
		}

		for (final String key : browserClientMap.keySet()) {
			Future<Void> taskFuture = null;
			try {
				taskFuture = exec.take();
				taskFuture.get(browserClientMap.get(key).getTimeout(),
						TimeUnit.SECONDS);
			} catch (Throwable e) {
				log.error("$$$ {} $$$", e.getCause().getMessage());
				e.printStackTrace();
				if (taskFuture != null) {
					taskFuture.cancel(true);
				}
			} finally {
				log.debug("+++ Ending browser #{} +++", key);
			}
		}
	}

	public static int getRampPlaytime(int numClients) {
		return numClients * clientRate + holdTime;
	}

}
