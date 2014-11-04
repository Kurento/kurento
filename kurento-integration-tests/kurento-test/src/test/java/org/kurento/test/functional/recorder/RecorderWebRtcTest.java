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
package org.kurento.test.functional.recorder;

import java.awt.Color;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.Shell;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.mediainfo.AssertMedia;

/**
 *
 * <strong>Description</strong>: Test of a HTTP Recorder, using the stream
 * source from a WebRtcEndpoint in loopback.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>WebRtcEndpoint -> WebRtcEndpoint & RecorderEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
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
public class RecorderWebRtcTest extends BrowserKurentoClientTest {

	private static final int PLAYTIME = 10; // seconds
	private static final int TIMEOUT_EOS = 60; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";
	private static final String PRE_PROCESS_SUFIX = "-preprocess.webm";

	@Test
	public void testRecorderWebRtcChrome() throws InterruptedException {
		doTest(Browser.CHROME, null, new Color(0, 135, 0));
	}

	public void doTest(Browser browserType, String video, Color color)
			throws InterruptedException {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		final String recordingPreProcess = FILE_SCHEMA
				+ getDefaultFileForRecording(PRE_PROCESS_SUFIX);
		final String recordingPostProcess = FILE_SCHEMA
				+ getDefaultFileForRecording();
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				recordingPreProcess).build();
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
			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);
			recorderEP.record();

			// Wait until event playing in the remote stream
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));

			// Guard time to play the video
			Thread.sleep(PLAYTIME * 1000);

			// Assert play time
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));

			// Assert color
			if (color != null) {
				Assert.assertTrue("The color of the video should be " + color,
						browser.similarColor(color));
			}
		}

		// Release Media Pipeline #1
		recorderEP.stop();
		mp.release();

		// Post-processing
		Shell.runAndWait("ffmpeg", "-i", recordingPreProcess, "-c", "copy",
				recordingPostProcess);

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp2,
				recordingPostProcess).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP.connect(webRtcEP2);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution #2. Play the recorded video
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);
			playerEP.play();

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			if (color != null) {
				Assert.assertTrue("The color of the video should be " + color,
						browser.similarColor(color));
			}
			Assert.assertTrue("Not received EOS event in player",
					eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));
			AssertMedia.assertCodecs(
					getDefaultFileForRecording(PRE_PROCESS_SUFIX),
					EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
		}

		// Release Media Pipeline #2
		mp2.release();
	}
}
