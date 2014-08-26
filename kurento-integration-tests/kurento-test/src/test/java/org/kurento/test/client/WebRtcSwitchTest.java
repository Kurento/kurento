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
package org.kurento.test.client;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;

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
 * <li>Browsers starts before default timeout</li>
 * <li>Color received by clients should be green (RGB #008700, video test of
 * Chrome)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class WebRtcSwitchTest extends BrowserKurentoClientTest {

	@Test
	public void testWebRtcSwitch() throws InterruptedException {
		// Media pipeline
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
			browser1.connectToWebRtcEndpoint(webRtcEndpoint1,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Delay time (to avoid the same timing in videos)
			Thread.sleep(1000);

			// Browser 2
			browser2.subscribeEvents("playing");
			browser2.connectToWebRtcEndpoint(webRtcEndpoint2,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Delay time (to avoid the same timing in videos)
			Thread.sleep(1000);

			// Browser 3
			browser3.subscribeEvents("playing");
			browser3.connectToWebRtcEndpoint(webRtcEndpoint3,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Wait until event playing in the remote streams
			Assert.assertTrue("Timeout waiting playing event",
					browser1.waitForEvent("playing"));
			Assert.assertTrue("Timeout waiting playing event",
					browser2.waitForEvent("playing"));
			Assert.assertTrue("Timeout waiting playing event",
					browser3.waitForEvent("playing"));

			// Guard time to see each browser in loopback
			Thread.sleep(4000);
			assertColor(browser1, browser2, browser3);

			// Switching (round #1)
			webRtcEndpoint1.connect(webRtcEndpoint2);
			webRtcEndpoint2.connect(webRtcEndpoint3);
			webRtcEndpoint3.connect(webRtcEndpoint1);
			assertColor(browser1, browser2, browser3);

			// Guard time to see switching #1
			Thread.sleep(4000);

			// Switching (round #2)
			webRtcEndpoint1.connect(webRtcEndpoint3);
			webRtcEndpoint2.connect(webRtcEndpoint1);
			webRtcEndpoint3.connect(webRtcEndpoint2);
			assertColor(browser1, browser2, browser3);

			// Guard time to see switching #2
			Thread.sleep(4000);
		}
	}

	public void assertColor(BrowserClient... browsers) {
		for (BrowserClient browser : browsers) {
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					browser.colorSimilarTo(new Color(0, 135, 0)));
		}
	}

}
