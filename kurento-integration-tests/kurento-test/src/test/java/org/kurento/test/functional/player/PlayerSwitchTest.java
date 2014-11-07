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
package org.kurento.test.functional.player;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * <strong>Description</strong>: Test of a N Players.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>5xPlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Play time should be the expected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class PlayerSwitchTest extends FunctionalTest {

	private static final int PLAYTIME = 30; // seconds
	private static final int N_PLAYER = 5;

	@Test
	public void testPlayerSwitchChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testPlayerSwitchFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/red.webm").build();
		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/green.webm").build();
		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/blue.webm").build();
		PlayerEndpoint playerBall = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/ball.webm").build();
		PlayerEndpoint playerRtsp = new PlayerEndpoint.Builder(
				mp,
				"rtsp://r6---sn-cg07luez.c.youtube.com/CiILENy73wIaGQm2gbECn1Hi5RMYDSANFEgGUgZ2aWRlb3MM/0/0/0/video.3gp")
				.build();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			// red
			playerRed.connect(webRtcEndpoint);
			playerRed.play();
			browser.subscribeEvents("playing");
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// green
			playerGreen.connect(webRtcEndpoint);
			playerGreen.play();
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// blue
			playerBlue.connect(webRtcEndpoint);
			playerBlue.play();
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// smpte
			playerBall.connect(webRtcEndpoint);
			playerBall.play();
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// ball
			playerRtsp.connect(webRtcEndpoint);
			playerRtsp.play();
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));
		}

		// Release Media Pipeline
		mp.release();
	}

}
