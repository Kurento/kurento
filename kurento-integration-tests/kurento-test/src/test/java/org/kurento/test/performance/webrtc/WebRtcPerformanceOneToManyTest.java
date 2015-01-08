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

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserRunner;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.services.Node;

/**
 * <strong>Description</strong>: WebRTC (one to many) test with Selenium Grid.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> NxWebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No assertion, just data gathering.</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcPerformanceOneToManyTest extends PerformanceTest {

	private static final int DEFAULT_VIEWERS = 1; // Number of viewers
	private static final int DEFAULT_NBROWSERS = 4; // Browser per node
	private static final int DEFAULT_CLIENT_RATE = 5000; // milliseconds
	private static final int DEFAULT_HOLD_TIME = 30000; // milliseconds

	private int holdTime;
	private Node master;

	public WebRtcPerformanceOneToManyTest() throws InterruptedException {

		int numViewers = Integer.parseInt(System.getProperty(
				"test.webrtcgrid.numnodes", String.valueOf(DEFAULT_VIEWERS)));

		int numBrowsers = Integer.parseInt(System.getProperty(
				"test.webrtcgrid.numbrowsers",
				String.valueOf(DEFAULT_NBROWSERS)));

		int clientRate = Integer.parseInt(System.getProperty(
				"test.webrtcgrid.clientrate",
				String.valueOf(DEFAULT_CLIENT_RATE)));

		holdTime = Integer.parseInt(System.getProperty(
				"test.webrtcgrid.holdtime", String.valueOf(DEFAULT_HOLD_TIME)));

		setBrowserCreationRate(clientRate);
		setNumBrowsersPerNode(numBrowsers);

		ArrayList<Node> nodes = new ArrayList<>();

		List<Node> viewers = getRandomNodes(numViewers, Browser.CHROME, null,
				null, numBrowsers);

		master = getRandomNodes(1, Browser.CHROME,
				getPathTestFiles() + "/video/15sec/rgbHD.y4m", null, 1).get(0);

		nodes.addAll(viewers);
		nodes.add(master);

		setNodes(nodes);

	}

	@Ignore
	@Test
	public void test() throws InterruptedException {

		// MASTER
		// Media Pipeline
		final MediaPipeline mp = kurentoClient.createMediaPipeline();
		final WebRtcEndpoint masterWebRtcEP = new WebRtcEndpoint.Builder(mp)
				.build();

		try (BrowserClient masterBrowser = new BrowserClient.Builder()
				.browser(master.getBrowser()).client(Client.WEBRTC)
				.video(master.getVideo()).build()) {

			masterBrowser.initWebRtc(masterWebRtcEP, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_ONLY);

			final int playTime = getAllBrowsersStartedTime() + holdTime;

			parallelBrowsers(new BrowserRunner() {
				public void run(BrowserClient browser, int num, String name)
						throws Exception {

					long endTimeMillis = System.currentTimeMillis() + playTime;

					try {

						// Media Pipeline
						WebRtcEndpoint viewerWebRtcEP = new WebRtcEndpoint.Builder(
								mp).build();

						masterWebRtcEP.connect(viewerWebRtcEP);

						log.debug("*** start#1 {}", name);
						browser.subscribeEvents("playing");
						log.debug("### start#2 {}", name);
						browser.initWebRtc(viewerWebRtcEP,
								WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);
						log.debug(">>> start#3 {}", name);

						browser.checkLatencyUntil(endTimeMillis);

					} catch (Throwable e) {
						log.error("[[[ {} ]]]", e.getCause().getMessage());
						throw e;
					}
				}
			});

		} finally {

			log.debug("<<< Releasing pipeline");

			// Release Media Pipeline
			if (mp != null) {
				mp.release();
			}
		}
	}
}
