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
import org.kurento.test.client.ConsoleLogLevel;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestConfig;
import org.kurento.test.config.TestScenario;

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

	public WebRtcSwitchTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {

		// Test: 1+nViewers local Chrome's
		TestScenario test = new TestScenario();
		test.addBrowser(TestConfig.PRESENTER + 1, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());
		test.addBrowser(TestConfig.PRESENTER + 2, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());
		test.addBrowser(TestConfig.PRESENTER + 3, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testWebRtcSwitchChrome() throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint3 = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint1.connect(webRtcEndpoint1);
		webRtcEndpoint2.connect(webRtcEndpoint2);
		webRtcEndpoint3.connect(webRtcEndpoint3);

		// Start WebRTC in loopback in each browser
		subscribeEvents(TestConfig.PRESENTER + 1, "playing");
		initWebRtc(TestConfig.PRESENTER + 1, webRtcEndpoint1,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

		// Delay time (to avoid the same timing in videos)
		Thread.sleep(1000);

		// Browser 2
		subscribeEvents(TestConfig.PRESENTER + 2, "playing");
		initWebRtc(TestConfig.PRESENTER + 2, webRtcEndpoint2,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

		// Delay time (to avoid the same timing in videos)
		Thread.sleep(1000);

		// Browser 3
		subscribeEvents(TestConfig.PRESENTER + 3, "playing");
		initWebRtc(TestConfig.PRESENTER + 3, webRtcEndpoint3,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

		// Wait until event playing in the remote streams
		Assert.assertTrue(
				"Not received media #1 (timeout waiting playing event)",
				waitForEvent(TestConfig.PRESENTER + 1, "playing"));
		Assert.assertTrue(
				"Not received media #2 (timeout waiting playing event)",
				waitForEvent(TestConfig.PRESENTER + 2, "playing"));
		Assert.assertTrue(
				"Not received media #3 (timeout waiting playing event)",
				waitForEvent(TestConfig.PRESENTER + 3, "playing"));

		// Guard time to see browsers
		Thread.sleep(PLAYTIME * 1000);
		assertColor();

		// Switching (round #1)
		webRtcEndpoint1.connect(webRtcEndpoint2);
		webRtcEndpoint2.connect(webRtcEndpoint3);
		webRtcEndpoint3.connect(webRtcEndpoint1);
		assertColor();
		consoleLog(TestConfig.PRESENTER + 1, ConsoleLogLevel.info,
				"Switch #1: webRtcEndpoint1 -> webRtcEndpoint2");
		consoleLog(TestConfig.PRESENTER + 2, ConsoleLogLevel.info,
				"Switch #1: webRtcEndpoint2 -> webRtcEndpoint3");
		consoleLog(TestConfig.PRESENTER + 3, ConsoleLogLevel.info,
				"Switch #1: webRtcEndpoint3 -> webRtcEndpoint1");

		// Guard time to see switching #1
		Thread.sleep(PLAYTIME * 1000);

		// Switching (round #2)
		webRtcEndpoint1.connect(webRtcEndpoint3);
		webRtcEndpoint2.connect(webRtcEndpoint1);
		webRtcEndpoint3.connect(webRtcEndpoint2);
		assertColor();
		consoleLog(TestConfig.PRESENTER + 1, ConsoleLogLevel.info,
				"Switch #2: webRtcEndpoint1 -> webRtcEndpoint3");
		consoleLog(TestConfig.PRESENTER + 2, ConsoleLogLevel.info,
				"Switch #2: webRtcEndpoint2 -> webRtcEndpoint1");
		consoleLog(TestConfig.PRESENTER + 3, ConsoleLogLevel.info,
				"Switch #2: webRtcEndpoint3 -> webRtcEndpoint2");

		// Guard time to see switching #2
		Thread.sleep(PLAYTIME * 1000);

		// Release Media Pipeline
		mp.release();
	}

	public void assertColor() {
		for (String key : testScenario.getBrowserMap().keySet()) {
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					similarColor(key, CHROME_VIDEOTEST_COLOR));
		}
	}

}
