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
package org.kurento.test.functional.webrtc;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.latency.LatencyController;

/**
 * <strong>Description</strong>: WebRTC one to many test.<br/>
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
public class WebRtcOneToManyTest extends FunctionalTest {

	private static final int PLAYTIME = 60; // seconds
	private static final int DEFAULT_NUM_VIEWERS = 2;

	private int numViewers;

	@Before
	public void setup() {
		numViewers = getProperty("test.webrtcone2many.numviewers",
				DEFAULT_NUM_VIEWERS);
	}

	@Test
	public void testWebRtcOneToManyChrome() throws InterruptedException,
			IOException {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws InterruptedException,
			IOException {
		// Media Pipeline
		final MediaPipeline mp = kurentoClient.createMediaPipeline();
		final WebRtcEndpoint masterWebRtcEP = new WebRtcEndpoint.Builder(mp)
				.build();

		// Browser builder
		final BrowserClient.Builder builder = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC);

		// Assets for viewers
		final BrowserClient[] viewers = new BrowserClient[numViewers];
		final LatencyController[] cs = new LatencyController[numViewers];
		final WebRtcEndpoint[] viewerWebRtcEPs = new WebRtcEndpoint[numViewers];
		final CountDownLatch latch = new CountDownLatch(numViewers);

		try (BrowserClient master = builder.video(
				getPathTestFiles() + "/video/15sec/rgbHD.y4m").build()) {

			// Master
			master.subscribeLocalEvents("playing");
			master.initWebRtc(masterWebRtcEP, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_ONLY);

			// Viewers
			for (int j = 0; j < viewers.length; j++) {
				final int i = j;
				new Thread() {
					public void run() {
						try {
							viewerWebRtcEPs[i] = new WebRtcEndpoint.Builder(mp)
									.build();
							masterWebRtcEP.connect(viewerWebRtcEPs[i]);
							viewers[i] = builder.build();
							viewers[i].subscribeEvents("playing");
							viewers[i].initWebRtc(viewerWebRtcEPs[i],
									WebRtcChannel.VIDEO_ONLY,
									WebRtcMode.RCV_ONLY);

							// Latency control
							String name = "viewer" + (i + 1);
							cs[i] = new LatencyController(name);
							cs[i].activateRemoteLatencyAssessmentIn(master,
									viewers[i]);
							cs[i].checkLatency(PLAYTIME, TimeUnit.SECONDS);
							cs[i].drawChart(getDefaultOutputFile("-" + name
									+ "-latency.png"), 500, 270);
							cs[i].writeCsv(getDefaultOutputFile("-" + name
									+ "-latency.csv"));
							cs[i].logLatencyErrorrs();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							latch.countDown();
						}
					}
				}.start();
			}

			// Wait to finish viewers threads
			latch.await();

		} finally {
			// Close viewers
			for (int i = 0; i < viewers.length; i++) {
				viewers[i].close();
			}

			// Release Media Pipeline
			mp.release();
		}
	}
}
