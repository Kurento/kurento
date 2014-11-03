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
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * <strong>Description</strong>: WebRTC in loopback.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Play time should be as expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */

public class WebRtcTest extends BrowserKurentoClientTest {

	private static final int PLAYTIME = 10; // seconds to play in WebRTC

	@Test
	public void testWebRtcLoopbackChrome() throws InterruptedException {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = new MediaPipeline.Builder(kurentoClient).build();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);

		try (BrowserClient browser = builder.build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			// Guard time to play the video
			Thread.sleep(PLAYTIME * 1000);

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					browser.similarColor(new Color(0, 135, 0)));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));
		}

		// Release Media Pipeline
		mp.release();
	}
}
