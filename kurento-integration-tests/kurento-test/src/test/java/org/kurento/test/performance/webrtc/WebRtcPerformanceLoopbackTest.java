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

	private static final int DEFAULT_NODES = 1; // Number of nodes
	private static final int DEFAULT_NBROWSERS = 2; // Browser per node
	private static final int DEFAULT_CLIENT_RATE = 5000; // milliseconds
	private static final int DEFAULT_HOLD_TIME = 10000; // milliseconds

	private int holdTime;

	public WebRtcPerformanceLoopbackTest() {

		int numNodes = getProperty("test.webrtcgrid.numnodes", DEFAULT_NODES);

		int numBrowsers = getProperty("test.webrtcgrid.numbrowsers",
				DEFAULT_NBROWSERS);

		holdTime = getProperty("test.webrtcgrid.holdtime", DEFAULT_HOLD_TIME);

		setNumBrowsersPerNode(numBrowsers);

		setBrowserCreationRate(getProperty("test.webrtcgrid.clientrate",
				DEFAULT_CLIENT_RATE));

		setNodes(getRandomNodes(numNodes, Browser.CHROME, getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m", null, numBrowsers));
	}

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
