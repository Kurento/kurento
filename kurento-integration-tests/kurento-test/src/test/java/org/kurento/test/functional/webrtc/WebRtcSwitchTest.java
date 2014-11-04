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

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.ConsoleLogLevel;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

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
public class WebRtcSwitchTest extends BrowserKurentoClientTest {

	private static final int PLAYTIME = 5; // seconds

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

		BrowserClient.Builder builderWebrtc = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.WEBRTC);

		try (BrowserClient browser1 = builderWebrtc.build();
				BrowserClient browser2 = builderWebrtc.build();
				BrowserClient browser3 = builderWebrtc.build()) {

			// Start WebRTC in loopback in each browser
			browser1.subscribeEvents("playing");
			browser1.initWebRtc(webRtcEndpoint1, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			// Delay time (to avoid the same timing in videos)
			Thread.sleep(1000);

			// Browser 2
			browser2.subscribeEvents("playing");
			browser2.initWebRtc(webRtcEndpoint2, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			// Delay time (to avoid the same timing in videos)
			Thread.sleep(1000);

			// Browser 3
			browser3.subscribeEvents("playing");
			browser3.initWebRtc(webRtcEndpoint3, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			// Wait until event playing in the remote streams
			Assert.assertTrue(
					"Not received media #1 (timeout waiting playing event)",
					browser1.waitForEvent("playing"));
			Assert.assertTrue(
					"Not received media #2 (timeout waiting playing event)",
					browser2.waitForEvent("playing"));
			Assert.assertTrue(
					"Not received media #3 (timeout waiting playing event)",
					browser3.waitForEvent("playing"));

			// Guard time to see browsers
			Thread.sleep(PLAYTIME * 1000);
			assertColor(browser1, browser2, browser3);

			// Switching (round #1)
			webRtcEndpoint1.connect(webRtcEndpoint2);
			webRtcEndpoint2.connect(webRtcEndpoint3);
			webRtcEndpoint3.connect(webRtcEndpoint1);
			assertColor(browser1, browser2, browser3);
			browser1.consoleLog(ConsoleLogLevel.info,
					"Switch #1: webRtcEndpoint1 -> webRtcEndpoint2");
			browser2.consoleLog(ConsoleLogLevel.info,
					"Switch #1: webRtcEndpoint2 -> webRtcEndpoint3");
			browser3.consoleLog(ConsoleLogLevel.info,
					"Switch #1: webRtcEndpoint3 -> webRtcEndpoint1");

			// Guard time to see switching #1
			Thread.sleep(PLAYTIME * 1000);

			// Switching (round #2)
			webRtcEndpoint1.connect(webRtcEndpoint3);
			webRtcEndpoint2.connect(webRtcEndpoint1);
			webRtcEndpoint3.connect(webRtcEndpoint2);
			assertColor(browser1, browser2, browser3);
			browser1.consoleLog(ConsoleLogLevel.info,
					"Switch #2: webRtcEndpoint1 -> webRtcEndpoint3");
			browser2.consoleLog(ConsoleLogLevel.info,
					"Switch #2: webRtcEndpoint2 -> webRtcEndpoint1");
			browser3.consoleLog(ConsoleLogLevel.info,
					"Switch #2: webRtcEndpoint3 -> webRtcEndpoint2");

			// Guard time to see switching #2
			Thread.sleep(PLAYTIME * 1000);
		}

		// Release Media Pipeline
		mp.release();
	}

	public void assertColor(BrowserClient... browsers) {
		for (BrowserClient browser : browsers) {
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					browser.similarColor(new Color(0, 135, 0)));
		}
	}

}
