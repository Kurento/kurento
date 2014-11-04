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
 * <strong>Description</strong>: Test of a Recorder switching sources from
 * PlayerEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time should be the expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderSwitchTest extends BrowserKurentoClientTest {

	private static final int PLAYTIME = 20; // seconds
	private static final int TIMEOUT_EOS = 60; // seconds
	private static final int N_PLAYER = 3;
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";
	private static final String PRE_PROCESS_SUFIX = "-preprocess.webm";

	@Test
	public void testRecorderSwitchChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testRecorderSwitchFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/red.webm").build();
		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/green.webm").build();
		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/blue.webm").build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		final String recordingPreProcess = FILE_SCHEMA
				+ getDefaultFileForRecording(PRE_PROCESS_SUFIX);
		final String recordingPostProcess = FILE_SCHEMA
				+ getDefaultFileForRecording();
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				recordingPreProcess).build();

		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			// red
			playerRed.connect(webRtcEP);
			playerRed.connect(recorderEP);
			playerRed.play();
			recorderEP.record();

			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// green
			playerGreen.connect(webRtcEP);
			playerGreen.connect(recorderEP);
			playerGreen.play();
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// blue
			playerBlue.connect(webRtcEP);
			playerBlue.connect(recorderEP);
			playerBlue.play();
			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);

			// Assertions
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));
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
			Assert.assertTrue("Recorded video first must be red",
					browser.similarColor(Color.RED));
			Assert.assertTrue("Recorded video second must be green",
					browser.similarColor(Color.GREEN));
			Assert.assertTrue("Recorded video third must be blue",
					browser.similarColor(Color.BLUE));
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
