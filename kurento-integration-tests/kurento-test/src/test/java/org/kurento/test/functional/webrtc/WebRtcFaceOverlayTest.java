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
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.TestConfig;
import org.kurento.test.config.TestScenario;

/**
 * <strong>Description</strong>: WebRTC to FaceOverlayFilter test.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> FaceOverlayFilter -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Play time should be as expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
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
		return TestScenario.localChrome();
	}

	@Test
	public void testWebRtcFaceOverlay() throws InterruptedException {
		int playTime = Integer.parseInt(System.getProperty(
				"test.webrtcfaceoverlay.playtime",
				String.valueOf(DEFAULT_PLAYTIME)));

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(mp)
				.build();
		webRtcEndpoint.connect(faceOverlayFilter);
		faceOverlayFilter.connect(webRtcEndpoint);

		// Start WebRTC
		subscribeEvents(TestConfig.DEFAULT_BROWSER, "playing");
		initWebRtc(TestConfig.DEFAULT_BROWSER, webRtcEndpoint,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

		// Guard time to play the video
		Thread.sleep(playTime * 1000);

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				waitForEvent(TestConfig.DEFAULT_BROWSER, "playing"));
		Assert.assertTrue(
				"The color of the video should be green (RGB #008700)",
				similarColor(TestConfig.DEFAULT_BROWSER, CHROME_VIDEOTEST_COLOR));
		double currentTime = getCurrentTime(TestConfig.DEFAULT_BROWSER);
		Assert.assertTrue("Error in play time (expected: " + playTime
				+ " sec, real: " + currentTime + " sec)",
				compare(TestConfig.DEFAULT_BROWSER, playTime, currentTime));

		// Release Media Pipeline
		mp.release();
	}
}
