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
package org.kurento.test.performance.webrtc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.VideoTag;
import org.kurento.test.monitor.SystemMonitor;
import org.kurento.test.services.Node;

/**
 * <strong>Description</strong>: WebRTC (in loopback) test with Selenium Grid.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No latency problems detected test startup</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcPerformanceLatencyTest extends PerformanceTest {

	private static final int DEFAULT_NODES = 10; // Number of nodes
	private static final int DEFAULT_NBROWSERS = 2; // Browser per node
	private static final int DEFAULT_CLIENT_RATE = 1000; // milliseconds
	private static final int DEFAULT_MONITOR_RATE = 1000; // milliseconds
	private static final int DEFAULT_MAX_LATENCY = 1; // seconds

	public int numBrowsers;
	public int numNodes;
	public int clientRate;

	public WebRtcPerformanceLatencyTest() {
		nodes = new ArrayList<Node>();

		numNodes = Integer.parseInt(System.getProperty(
				"test.webrtcgrid.numnodes", String.valueOf(DEFAULT_NODES)));
		numBrowsers = Integer.parseInt(System.getProperty(
				"test.webrtcgrid.numbrowsers",
				String.valueOf(DEFAULT_NBROWSERS)));
		clientRate = Integer.parseInt(System.getProperty(
				"test.webrtcgrid.clientrate",
				String.valueOf(DEFAULT_CLIENT_RATE)));

		nodes = getRandomNodes(numNodes, Browser.CHROME, getPathTestFiles()
				+ "/video/15sec/rgb.y4m", null, numBrowsers);
	}

	private SystemMonitor monitor;

	@Before
	public void setup() {
		monitor = new SystemMonitor(DEFAULT_MONITOR_RATE);
		monitor.start();
	}

	@After
	public void teardown() throws IOException {
		monitor.stop();
		monitor.writeResults(getDefaultOutputFile("-kms-monitor.csv"));
	}

	@Test
	public void tesWebRtcGridChrome() throws InterruptedException,
			ExecutionException, IOException {

		final int playTime = numNodes * numBrowsers * clientRate;

		ExecutorService internalExec = Executors.newFixedThreadPool(numNodes
				* numBrowsers);
		CompletionService<Void> exec = new ExecutorCompletionService<>(
				internalExec);

		for (final Node node : nodes) {
			for (int i = 0; i < numBrowsers; i++) {
				final String name = node.getAddress() + "-browser" + i;
				monitor.incrementNumClients();
				exec.submit(new Callable<Void>() {
					public Void call() throws IOException {
						doTest(node, playTime, name);
						monitor.decrementNumClients();
						return null;
					}
				});
				Thread.sleep(clientRate);
			}
		}

		for (int i = 0; i < numNodes * numBrowsers; i++) {
			try {
				exec.take().get();
			} catch (ExecutionException e) {
				internalExec.shutdownNow();
				monitor.incrementLatencyErrors();
				log.error(e.getCause().getMessage());
			}
		}
	}

	public void doTest(Node node, int playTime, String name) throws IOException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder()
				.browser(node.getBrowser()).client(Client.WEBRTC)
				.remoteNode(node);
		if (node.getVideo() != null) {
			builder = builder.video(node.getVideo());
		}

		try (BrowserClient browser = builder.build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_RCV);

			// Latency control
			LatencyController cs = new LatencyController(name, monitor);
			cs.setLatencyThreshold(DEFAULT_MAX_LATENCY, TimeUnit.SECONDS);
			browser.addChangeColorEventListener(VideoTag.LOCAL, cs, name
					+ "-loc");
			browser.addChangeColorEventListener(VideoTag.REMOTE, cs, name
					+ "-rem");
			cs.checkLatency(playTime, TimeUnit.MILLISECONDS);

			// Draw latency log
			cs.logLatencyErrorrs();
		}

		// Release Media Pipeline
		mp.release();
	}

}
