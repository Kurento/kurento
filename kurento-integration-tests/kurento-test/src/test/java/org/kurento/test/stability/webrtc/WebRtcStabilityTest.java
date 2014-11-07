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
package org.kurento.test.stability.webrtc;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * <strong>Description</strong>: Stability test for WebRTC in loopback during a
 * long time (configurable).<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Color of the video should be the expected</li>
 * <li>Play time should be as expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */

public class WebRtcStabilityTest extends StabilityTest {

	private static final int DEFAULT_PLAYTIME = 1; // minutes

	@Test
	public void testWebRtcStabilityChrome() throws InterruptedException {
		final int playTime = Integer.parseInt(System.getProperty(
				"test.webrtcstability.playtime",
				String.valueOf(DEFAULT_PLAYTIME)));
		doTest(Browser.CHROME, playTime);
	}

	public void doTest(Browser browserType, int playTime)
			throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);

		try (BrowserClient browser = builder.build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			// Assertion #1 : receive media
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));

			// Play the video during playtime
			long systemPlayTime = System.currentTimeMillis() + playTime * 60000;

			for (int i = 0;; i++) {
				// Assertion #2 (each second) : received color should be as
				// expected
				Assert.assertTrue(
						"The color of the video should be green. Difference detected at second "
								+ i,
						browser.similarColor(CHROME_VIDEOTEST_COLOR));

				Thread.sleep(1000);
				if (System.currentTimeMillis() > systemPlayTime) {
					break;
				}
			}

			// Assertion #3 : playtime shoudl be as expected
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + playTime
					+ " minutes, real: " + currentTime + " minutes)",
					compare(playTime * 60, currentTime));
		}

		// Release Media Pipeline
		mp.release();
	}
}
