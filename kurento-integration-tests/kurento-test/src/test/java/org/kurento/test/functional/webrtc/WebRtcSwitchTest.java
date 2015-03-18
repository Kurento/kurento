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
package org.kurento.test.functional.webrtc;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.ConsoleLogLevel;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.TestScenario;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * <strong>Description</strong>: Back-To-Back WebRTC switch. Three clients:
 * A,B,C sets up WebRTC send-recv with audio/video. Switch between following
 * scenarios: A<->B, A<->C, B<->C. At least two rounds. <br/>
 * <strong>Pipeline(s)</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class WebRtcSwitchTest extends FunctionalTest {

	private static final int PLAYTIME = 5; // seconds
	private static final int NUM_BROWSERS = 3;

	public WebRtcSwitchTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		// Test: NUM_BROWSERS local Chrome's
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.BROWSER, new BrowserClient.Builder()
				.client(Client.WEBRTC).browserType(BrowserType.CHROME)
				.numInstances(NUM_BROWSERS).scope(BrowserScope.LOCAL).build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testWebRtcSwitchChrome() throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoints[] = new WebRtcEndpoint[NUM_BROWSERS];

		for (int i = 0; i < NUM_BROWSERS; i++) {
			webRtcEndpoints[i] = new WebRtcEndpoint.Builder(mp).build();
			webRtcEndpoints[i].connect(webRtcEndpoints[i]);

			// Start WebRTC in loopback in each browser
			getBrowser(i).subscribeEvents("playing");
			getBrowser(i).initWebRtc(webRtcEndpoints[i],
					WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

			// Delay time (to avoid the same timing in videos)
			Thread.sleep(TimeUnit.SECONDS.toMillis(1));

			// Wait until event playing in the remote streams
			Assert.assertTrue(
					"Not received media #1 (timeout waiting playing event)",
					getBrowser(i).waitForEvent("playing"));

			// Assert color
			assertColor(i);
		}

		// Guard time to see switching #0
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Switching (round #1)
		for (int i = 0; i < NUM_BROWSERS; i++) {
			int next = (i + 1) >= NUM_BROWSERS ? 0 : i + 1;
			webRtcEndpoints[i].connect(webRtcEndpoints[next]);
			getBrowser(i).consoleLog(
					ConsoleLogLevel.info,
					"Switch #1: webRtcEndpoint" + i + " -> webRtcEndpoint"
							+ next);
			// Assert color
			assertColor(i);
		}

		// Guard time to see switching #1
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Switching (round #2)
		for (int i = 0; i < NUM_BROWSERS; i++) {
			int previous = (i - 1) < 0 ? NUM_BROWSERS - 1 : i - 1;
			webRtcEndpoints[i].connect(webRtcEndpoints[previous]);
			getBrowser(i).consoleLog(
					ConsoleLogLevel.info,
					"Switch #2: webRtcEndpoint" + i + " -> webRtcEndpoint"
							+ previous);
			// Assert color
			assertColor(i);
		}

		// Guard time to see switching #2
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		// Release Media Pipeline
		mp.release();
	}

	public void assertColor(int index) {
		Assert.assertTrue(
				"The color of the video should be green (RGB #008700)",
				getBrowser(index).similarColor(CHROME_VIDEOTEST_COLOR));

	}

}
