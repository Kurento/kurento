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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.TestScenario;

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

	public PlayerSwitchTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testPlayerSwitch() throws Exception {
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
		getBrowser().subscribeEvents("playing");
		getBrowser().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);

		// red
		playerRed.connect(webRtcEndpoint);
		playerRed.play();
		getBrowser().subscribeEvents("playing");
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// green
		playerGreen.connect(webRtcEndpoint);
		playerGreen.play();
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// blue
		playerBlue.connect(webRtcEndpoint);
		playerBlue.play();
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// smpte
		playerBall.connect(webRtcEndpoint);
		playerBall.play();
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// ball
		playerRtsp.connect(webRtcEndpoint);
		playerRtsp.play();
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getBrowser().waitForEvent("playing"));
		double currentTime = getBrowser().getCurrentTime();
		Assert.assertTrue("Error in play time (expected: " + PLAYTIME
				+ " sec, real: " + currentTime + " sec)",
				getBrowser().compare(PLAYTIME, currentTime));

		// Release Media Pipeline
		mp.release();
	}

}
