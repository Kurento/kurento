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

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * WebRTC in loopback with a FaceOverlayFilter. <br>
 *
 * Media Pipeline(s): <br>
 * · WebRtcEndpoint -> FaceOverlayFilter -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) WebRtcEndpoint in loopback with a FaceOverlayFilter <br>
 * 2. (Browser) WebRtcPeer in send-receive mode and receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · Play time in remote video should be as expected <br>
 * · The color of the received video should be as expected <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */

public class WebRtcFaceOverlayTest extends FunctionalTest {

	private static final int DEFAULT_PLAYTIME = 10; // seconds

	public WebRtcFaceOverlayTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testWebRtcFaceOverlay() throws InterruptedException {
		int playTime = Integer
				.parseInt(System.getProperty("test.webrtcfaceoverlay.playtime",
						String.valueOf(DEFAULT_PLAYTIME)));

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(mp)
				.build();
		webRtcEndpoint.connect(faceOverlayFilter);
		faceOverlayFilter.connect(webRtcEndpoint);

		// Start WebRTC and wait for playing event
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_RCV);
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));

		// Guard time to play the video
		waitSeconds(playTime);

		// Assertions
		double currentTime = getPage().getCurrentTime();
		Assert.assertTrue(
				"Error in play time (expected: " + playTime + " sec, real: "
						+ currentTime + " sec)",
				getPage().compare(playTime, currentTime));
		Assert.assertTrue("The color of the video should be green",
				getPage().similarColor(CHROME_VIDEOTEST_COLOR));

		// Release Media Pipeline
		mp.release();
	}
}
