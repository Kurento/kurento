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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserRunner;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.grid.ParallelBrowsers;

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

	private static final String NUM_VIEWERS_PROPERTY = "perf.loopback.numviewers";
	private static final int NUM_VIEWERS_DEFAULT = 1;

	private static final String BROWSER_PER_VIEWER_PROPERTY = "perf.loopback.browserperviewer";
	private static final int BROWSER_PER_VIEWER_DEFAULT = 2;

	private static int numViewers;
	private static int browserPerViewer;

	public WebRtcPerformanceLoopbackTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		numViewers = getProperty(NUM_VIEWERS_PROPERTY, NUM_VIEWERS_DEFAULT);
		browserPerViewer = getProperty(BROWSER_PER_VIEWER_PROPERTY,
				BROWSER_PER_VIEWER_DEFAULT);

		TestScenario test = new TestScenario();
		String video = getPathTestFiles() + "/video/15sec/rgbHD.y4m";
		test.addBrowser(
				BrowserConfig.VIEWER,
				new BrowserClient.Builder().client(Client.WEBRTC)
						.numInstances(numViewers)
						.browserPerInstance(browserPerViewer)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.LOCAL).video(video).build());

		// Uncomment this for remote scenario
		// test.addBrowser(TestConfig.VIEWER, new BrowserClient.Builder()
		// .numInstances(numViewers).browserPerInstance(browserPerViewer)
		// .browserType(BrowserType.CHROME).scope(BrowserScope.REMOTE)
		// .video(video).build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testWebRtcPerformanceLoopback() throws Exception {
		Map<String, BrowserClient> browsers = getTestScenario().getBrowserMap();

		final int playTime = ParallelBrowsers.getRampPlaytime(browsers.size());

		ParallelBrowsers.ramp(browsers, monitor, new BrowserRunner() {
			public void run(BrowserClient browser) throws Exception {

				long endTimeMillis = System.currentTimeMillis() + playTime;
				String name = browser.getId();
				MediaPipeline mp = null;

				try {
					// Media Pipeline
					mp = kurentoClient.createMediaPipeline();
					WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(
							mp).build();
					webRtcEndpoint.connect(webRtcEndpoint);

					log.debug(">>> start {}", name);
					getBrowser(name).subscribeEvents("playing");
					getBrowser(name).initWebRtc(webRtcEndpoint,
							WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);
					getBrowser(name).activateRemoteRtcStats(monitor,
							"webRtcPeer.peerConnection");
					getBrowser(name).checkLatencyUntil(monitor, endTimeMillis);

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
