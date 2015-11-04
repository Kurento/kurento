/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.functional.player;

import java.awt.Color;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Test of a the pause feature for a PlayerEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) During the playback of a stream from a PlayerEndpoint to a
 * WebRtcEndpoint, the PlayerEndpoint is paused and then resumed <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Color or the video should remain when a video is paused <br>
 * · After the pause, the color or the video should change <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class PlayerPauseTest extends FunctionalTest {

	public PlayerPauseTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testPlayerPause() throws Exception {
		// Test data
		final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.webm";
		final Color[] expectedColors = { Color.RED, Color.GREEN, Color.BLUE };
		final int pauseTimeSeconds = 10;

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, mediaUrl)
				.build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
		playerEP.connect(webRtcEP);

		// Test execution
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);
		playerEP.play();

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));
		Assert.assertTrue(
				"At the beginning, the color of the video should be "
						+ expectedColors[0],
				getPage().similarColor(expectedColors[0]));

		// Pause stream and wait pauseTimeSeconds seconds
		playerEP.pause();
		Thread.sleep(TimeUnit.SECONDS.toMillis(pauseTimeSeconds));

		// Resume video after the pause, video should be still red, and then
		// green and blue
		playerEP.play();
		Assert.assertTrue(
				"After the pause, the color of the video should be remain "
						+ expectedColors[0],
				getPage().similarColor(expectedColors[0]));
		Assert.assertTrue(
				"After the pause, the color of the video should continue "
						+ expectedColors[1],
				getPage().similarColor(expectedColors[1]));
		Assert.assertTrue(
				"After the pause, the color of the video should end "
						+ expectedColors[2],
				getPage().similarColor(expectedColors[2]));

		// Release Media Pipeline
		mp.release();
	}
}
