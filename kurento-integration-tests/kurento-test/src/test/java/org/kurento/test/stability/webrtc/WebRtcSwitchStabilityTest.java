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
 * <strong>Description</strong>: Stability test for switching 2 WebRTC (looback
 * to back-2-back) a configurable number of times (each switch holds 1 second).<br/>
 * <strong>Pipeline(s)</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * <li>... to:</li>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (back to back)</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */

public class WebRtcSwitchStabilityTest extends StabilityTest {

	private static final int DEFAULT_NUM_SWITCH = 10;

	@Test
	public void testWebRtcSwitchChrome() throws InterruptedException {
		final int numSwitch = Integer.parseInt(System.getProperty(
				"test.webrtcstability.switch",
				String.valueOf(DEFAULT_NUM_SWITCH)));
		doTest(Browser.CHROME, numSwitch);
	}

	public void doTest(Browser browserType, int numSwitch)
			throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint1.connect(webRtcEndpoint1);
		webRtcEndpoint2.connect(webRtcEndpoint2);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);

		try (BrowserClient browser1 = builder.build();
				BrowserClient browser2 = builder.build()) {
			browser1.subscribeEvents("playing");
			browser1.initWebRtc(webRtcEndpoint1, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);
			browser2.subscribeEvents("playing");
			browser2.initWebRtc(webRtcEndpoint2, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			// Assertion #1 : receive media
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser1.waitForEvent("playing"));
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser2.waitForEvent("playing"));

			for (int i = 0; i < numSwitch; i++) {
				// Assertion #2 (each switch) : received color should be as
				// expected
				Assert.assertTrue(
						"The color of the video in browser #1 should be green."
								+ " Difference detected at switch #" + (i + 1),
						browser1.similarColor(CHROME_VIDEOTEST_COLOR));
				Assert.assertTrue(
						"The color of the video in browser #2 should be green."
								+ " Difference detected at switch #" + (i + 1),
						browser2.similarColor(CHROME_VIDEOTEST_COLOR));

				Thread.sleep(1000);
				if (i % 2 == 0) {
					log.debug("Switch #" + (i + 1) + ": B2B");
					webRtcEndpoint1.connect(webRtcEndpoint2);
					webRtcEndpoint2.connect(webRtcEndpoint1);
				} else {
					log.debug("Switch #" + (i + 1) + ": loopback");
					webRtcEndpoint1.connect(webRtcEndpoint1);
					webRtcEndpoint2.connect(webRtcEndpoint2);
				}
			}
		}

		// Release Media Pipeline
		mp.release();
	}
}
