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

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserRunner;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * <strong>Description</strong>: WebRTC (in loopback) test with Selenium Grid.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No assertion, just data gathering.</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcPerformanceLoopbackTest extends PerformanceTest {

	// Number of nodes
	private static final String NUM_VIEWERS_PROPERTY = "perf.loopback.numnodes";
	private static final int NUM_VIEWERS_DEFAULT = 1;

	// Browser per node
	private static final String NUM_BROWSERS_PROPERTY = "perf.loopback.numbrowsers";
	private static final int NUM_BROWSERS_DEFAULT = 2;

	// Client rate in milliseconds
	private static final String CLIENT_RATE_PROPERTY = "perf.loopback.clientrate";
	private static final int CLIENT_RATE_DEFAULT = 5000;

	// Hold time in milliseconds
	private static final String HOLD_TIME_PROPERTY = "perf.loopback.holdtime";
	private static final int HOLD_TIME_DEFAULT = 10000;

	private int holdTime;

	public WebRtcPerformanceLoopbackTest() {

		int numNodes = getProperty(NUM_VIEWERS_PROPERTY, NUM_VIEWERS_DEFAULT);

		int numBrowsers = getProperty(NUM_BROWSERS_PROPERTY,
				NUM_BROWSERS_DEFAULT);

		holdTime = getProperty(HOLD_TIME_PROPERTY, HOLD_TIME_DEFAULT);

		setNumBrowsersPerNode(numBrowsers);

		setBrowserCreationRate(getProperty(CLIENT_RATE_PROPERTY,
				CLIENT_RATE_DEFAULT));

		setNodes(getRandomNodes(numNodes, Browser.CHROME, getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m", null, numBrowsers));
	}

	@Ignore
	@Test
	public void tesWebRtcGridChrome() throws InterruptedException, IOException {

		final int playTime = getAllBrowsersStartedTime() + holdTime;

		parallelBrowsers(new BrowserRunner() {
			public void run(BrowserClient browser, int num, String name)
					throws Exception {

				long endTimeMillis = System.currentTimeMillis() + playTime;

				MediaPipeline mp = null;

				try {
					// Media Pipeline
					mp = kurentoClient.createMediaPipeline();
					WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(
							mp).build();
					webRtcEndpoint.connect(webRtcEndpoint);

					log.debug("*** start#1 {}", name);
					browser.subscribeEvents("playing");
					log.debug("### start#2 {}", name);
					browser.initWebRtc(webRtcEndpoint,
							WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);
					log.debug(">>> start#3 {}", name);

					browser.checkLatencyUntil(endTimeMillis);

				} catch (Throwable e) {
					log.error("[[[ {} ]]]", e.getCause().getMessage());
					throw e;
				} finally {
					log.debug("<<< finally {}", name);

					// Release Media Pipeline
					if (mp != null) {
						mp.release();
					}
				}
			}
		});
	}

}
