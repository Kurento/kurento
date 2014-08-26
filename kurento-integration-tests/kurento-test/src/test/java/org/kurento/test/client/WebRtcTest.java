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
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.services.AudioChannel;
import org.kurento.test.services.Recorder;

/**
 * <strong>Description</strong>: WebRTC in loopback using custom video and audio
 * files.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser should start before default timeout</li>
 * <li>Play time should be as expected</li>
 * <li>Color received by client should be as expected</li>
 * <li>Perceived audio quality should be fair (PESQMOS)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */

public class WebRtcTest extends BrowserKurentoClientTest {

	private static int PLAYTIME = 10; // seconds to play in WebRTC
	private static int AUDIO_SAMPLE_RATE = 16000; // samples per second
	private static float MIN_PESQ_MOS = 3; // Audio quality (PESQ MOS [1..5])

	@Ignore
	@Test
	public void testWebRtcLoopbackChrome() throws InterruptedException {
		doTest(Browser.CHROME, getPathTestFiles() + "/video/10sec/red.y4m",
				"http://files.kurento.org/audio/10sec/fiware_mono_16khz.wav",
				Color.RED);
	}

	public void doTest(Browser browserType, String videoPath, String audioUrl,
			Color color) throws InterruptedException {
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);
		if (videoPath != null) {
			builder = builder.video(videoPath);
		}
		if (audioUrl != null) {
			builder = builder.audio(audioUrl, PLAYTIME, AUDIO_SAMPLE_RATE,
					AudioChannel.MONO);
		}

		try (BrowserClient browser = builder.build()) {
			browser.subscribeEvents("playing");
			browser.connectToWebRtcEndpoint(webRtcEndpoint,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Wait until event playing in the remote stream
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// Guard time to play the video
			Thread.sleep(PLAYTIME * 1000);

			// Assert play time
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time of HTTP player (expected: "
					+ PLAYTIME + " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));

			// Assert color
			if (color != null) {
				Assert.assertTrue("The color of the video should be " + color,
						browser.colorSimilarTo(color));
			}
		}

		// Assert audio quality
		if (audioUrl != null) {
			float realPesqMos = Recorder
					.getPesqMos(audioUrl, AUDIO_SAMPLE_RATE);
			Assert.assertTrue(
					"Bad perceived audio quality: PESQ MOS too low (expected="
							+ MIN_PESQ_MOS + ", real=" + realPesqMos + ")",
					realPesqMos >= MIN_PESQ_MOS);
		}
	}
}
