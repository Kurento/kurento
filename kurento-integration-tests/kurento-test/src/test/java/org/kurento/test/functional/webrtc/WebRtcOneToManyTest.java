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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestConfig;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;

/**
 * <strong>Description</strong>: WebRTC one to many test.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag of viewers</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcOneToManyTest extends FunctionalTest {

	private static final int PLAYTIME = 60; // seconds
	private static final int DEFAULT_NUM_VIEWERS = 2;
	private static int numViewers;

	public WebRtcOneToManyTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		numViewers = getProperty("test.webrtcone2many.numviewers",
				DEFAULT_NUM_VIEWERS);

		// Test: 1+nViewers local Chrome's
		TestScenario test = new TestScenario();
		test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.video(getPathTestFiles() + "/video/15sec/rgbHD.y4m").build());

		for (int i = 0; i < numViewers; i++) {
			test.addBrowser(TestConfig.VIEWER + i, new BrowserClient.Builder()
					.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
					.build());
		}
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testWebRtcOneToManyChrome() throws InterruptedException,
			IOException {
		// Media Pipeline
		final MediaPipeline mp = kurentoClient.createMediaPipeline();
		final WebRtcEndpoint masterWebRtcEP = new WebRtcEndpoint.Builder(mp)
				.build();

		// Assets for viewers
		final LatencyController[] cs = new LatencyController[numViewers];
		final WebRtcEndpoint[] viewerWebRtcEPs = new WebRtcEndpoint[numViewers];
		final CountDownLatch latch = new CountDownLatch(numViewers);

		// Master
		subscribeLocalEvents(TestConfig.PRESENTER, "playing");
		initWebRtc(TestConfig.PRESENTER, masterWebRtcEP,
				WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_ONLY);

		// Viewers
		for (int j = 0; j < numViewers; j++) {
			final int i = j;
			new Thread() {
				public void run() {
					try {
						viewerWebRtcEPs[i] = new WebRtcEndpoint.Builder(mp)
								.build();
						masterWebRtcEP.connect(viewerWebRtcEPs[i]);
						subscribeEvents(TestConfig.VIEWER + i, "playing");
						initWebRtc(TestConfig.VIEWER + i, viewerWebRtcEPs[i],
								WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);

						// Latency control
						String name = "viewer" + (i + 1);
						cs[i] = new LatencyController(name);
						cs[i].activateRemoteLatencyAssessmentIn(
								testScenario.getBrowserMap().get(
										TestConfig.PRESENTER),
								testScenario.getBrowserMap().get(
										TestConfig.VIEWER + i));
						cs[i].checkLatency(PLAYTIME, TimeUnit.SECONDS);
						cs[i].drawChart(getDefaultOutputFile("-" + name
								+ "-latency.png"), 500, 270);
						cs[i].writeCsv(getDefaultOutputFile("-" + name
								+ "-latency.csv"));
						cs[i].logLatencyErrorrs();

						// Assertions
						Assert.assertTrue(
								"Not received media (timeout waiting playing event)",
								waitForEvent(TestConfig.VIEWER + i, "playing"));
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

		// Release Media Pipeline
		mp.release();
	}
}
