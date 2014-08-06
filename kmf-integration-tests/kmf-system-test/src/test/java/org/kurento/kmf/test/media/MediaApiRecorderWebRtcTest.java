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
package org.kurento.kmf.test.media;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

import org.kurento.kmf.media.HttpGetEndpoint;
import org.kurento.kmf.media.MediaPipeline;
import org.kurento.kmf.media.PlayerEndpoint;
import org.kurento.kmf.media.RecorderEndpoint;
import org.kurento.kmf.media.WebRtcEndpoint;
import org.kurento.kmf.test.base.BrowserMediaApiTest;
import org.kurento.kmf.test.client.Browser;
import org.kurento.kmf.test.client.BrowserClient;
import org.kurento.kmf.test.client.Client;
import org.kurento.kmf.test.client.WebRtcChannel;
import org.kurento.kmf.test.mediainfo.AssertMedia;

/**
 * 
 * <strong>Description</strong>: Test of a HTTP Recorder, using the stream
 * source from a WebRtcEndpoint in loopback.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>WebRtcEndpoint -> WebRtcEndpoint & RecorderEndpoint</li>
 * <li>PlayerEndpoint -> HttpGetEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Play time should be the expected</li>
 * <li>Color of the video should be the expected</li>
 * <li>Browser ends before default timeout</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiRecorderWebRtcTest extends BrowserMediaApiTest {

	private static int PLAYTIME = 5; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";

	@Test
	public void testRecorderWebRtcChrome() throws InterruptedException {
		doTest(Browser.CHROME, null, new Color(0, 135, 0));
	}

	public void doTest(Browser browserType, String video, Color color)
			throws InterruptedException {
		// Media Pipeline #1
		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEP = mp.newWebRtcEndpoint().build();
		RecorderEndpoint recorderEP = mp.newRecorderEndpoint(
				FILE_SCHEMA + getDefaultFileForRecording()).build();
		webRtcEP.connect(webRtcEP);
		webRtcEP.connect(recorderEP);

		// Test execution #1. WewbRTC in loopback while it is recorded
		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);
		if (video != null) {
			builder = builder.video(video);
		}

		try (BrowserClient browser = builder.build()) {
			browser.subscribeEvents("playing");
			browser.connectToWebRtcEndpoint(webRtcEP,
					WebRtcChannel.AUDIO_AND_VIDEO);
			recorderEP.record();

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

		// Stop and release media elements
		recorderEP.stop();
		webRtcEP.release();
		recorderEP.release();

		// Media Pipeline #2
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				FILE_SCHEMA + getDefaultFileForRecording()).build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP.connect(httpEP);

		// Test execution #2. Play the recorded video
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Play time must be at least " + PLAYTIME
					+ " seconds and is " + currentTime + " seconds",
					currentTime >= PLAYTIME);
			if (color != null) {
				Assert.assertTrue("The color of the video should be " + color,
						browser.colorSimilarTo(color));
			}

			// Assess video/audio codec of the recorded video
			AssertMedia.assertCodecs(getDefaultFileForRecording(),
					EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
		}
	}
}
