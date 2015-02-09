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
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.Shell;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.mediainfo.AssertMedia;

/**
 *
 * <strong>Description</strong>: Test of a Recorder, using the stream source
 * from a PlayerEndpoint with FaceOverlayFilter through an WebRtcEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> FaceOverlayFilter -> RecorderEndpoint & WebRtcEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Color above the head of the video should be the expected (image overlaid)
 * </li>
 * <li>Media should be received in the video tag (in the recording)</li>
 * <li>Color of the video should be the expected (in the recording)</li>
 * <li>Ended event should arrive to player (in the recording)</li>
 * <li>Play time should be the expected (in the recording)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderFaceOverlayTest extends FunctionalTest {

	private static final int PLAYTIME = 30; // seconds
	private static final int TIMEOUT = 120; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";
	private static final String PRE_PROCESS_SUFIX = "-preprocess.webm";
	private static final Color EXPECTED_COLOR = Color.RED;
	private static final int EXPECTED_COLOR_X = 420;
	private static final int EXPECTED_COLOR_Y = 45;

	@Test
	public void testRecorderFaceOverlayChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testRecorderFaceOverlayFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/fiwarecut.mp4").build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();

		final String recordingPreProcess = FILE_SCHEMA
				+ getDefaultOutputFile(PRE_PROCESS_SUFIX);
		final String recordingPostProcess = FILE_SCHEMA
				+ getDefaultFileForRecording();
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				recordingPreProcess).build();
		FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp).build();
		filter.setOverlayedImage(
				"http://files.kurento.org/imgs/red-square.png", -0.2F, -1.2F,
				1.6F, 1.6F);

		playerEP.connect(filter);
		filter.connect(webRtcEP1);
		filter.connect(recorderEP);

		// Test execution #1. Play and record
		launchBrowser(browserType, webRtcEP1, playerEP, recorderEP);

		// Release Media Pipeline #1
		recorderEP.stop();
		mp.release();

		// Post-processing
		Shell.runAndWait("ffmpeg", "-i", recordingPreProcess, "-c", "copy",
				recordingPostProcess);

		// Play the recording
		playFileAsLocal(browserType, recordingPostProcess, PLAYTIME,
				EXPECTED_COLOR_X, EXPECTED_COLOR_Y, EXPECTED_COLOR);

		// Uncomment this line to play the recording with a new pipeline
		// playFileWithPipeline(browserType, recordingPostProcess, PLAYTIME,
		// EXPECTED_COLOR_X, EXPECTED_COLOR_Y, EXPECTED_COLOR);
	}

	private void launchBrowser(Browser browserType, WebRtcEndpoint webRtcEP,
			PlayerEndpoint playerEP, RecorderEndpoint recorderEP)
			throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {
			browser.setTimeout(TIMEOUT);
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);
			final CountDownLatch eosLatch = new CountDownLatch(1);
			playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
				@Override
				public void onEvent(EndOfStreamEvent event) {
					eosLatch.countDown();
				}
			});
			if (recorderEP != null) {
				recorderEP.record();
			}
			playerEP.play();

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			Assert.assertTrue(
					"Color above the head must be red (FaceOverlayFilter)",
					browser.similarColorAt(EXPECTED_COLOR, EXPECTED_COLOR_X,
							EXPECTED_COLOR_Y));
			Assert.assertTrue("Not received EOS event in player",
					eosLatch.await(TIMEOUT, TimeUnit.SECONDS));

			if (recorderEP != null) {
				AssertMedia.assertCodecs(
						getDefaultOutputFile(PRE_PROCESS_SUFIX),
						EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
			}
		}
	}
}