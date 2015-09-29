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
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.Shell;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 *
 * <strong>Description</strong>: Test of a Recorder, using the stream source
 * from a PlayerEndpoint with FaceOverlayFilter through an WebRtcEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> FaceOverlayFilter -> RecorderEndpoint & WebRtcEndpoint
 * </li>
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
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderFaceOverlayTest extends FunctionalTest {

	private static final int PLAYTIME = 30; // seconds
	private static final int THRESHOLD = 20; // seconds
	private static final String EXPECTED_VIDEO_CODEC = "VP8";
	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";
	private static final String PRE_PROCESS_SUFIX = "-preprocess.webm";
	private static final Color EXPECTED_COLOR = Color.RED;
	private static final int EXPECTED_COLOR_X = 420;
	private static final int EXPECTED_COLOR_Y = 45;

	public RecorderFaceOverlayTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testRecorderFaceOverlay() throws Exception {
		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/fiwarecut.mp4")
				.build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();

		final String recordingPreProcess = Protocol.FILE + getDefaultOutputFile(PRE_PROCESS_SUFIX);
		final String recordingPostProcess = Protocol.FILE + getDefaultFileForRecording();
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp, recordingPreProcess).build();
		FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp).build();
		filter.setOverlayedImage("http://files.kurento.org/imgs/red-square.png", -0.2F, -1.2F, 1.6F, 1.6F);

		playerEP.connect(filter);
		filter.connect(webRtcEP1);
		filter.connect(recorderEP);

		// Test execution #1. Play and record
		launchBrowser(webRtcEP1, playerEP, recorderEP);

		// Release Media Pipeline #1
		recorderEP.stop();
		mp.release();

		// Reloading browser
		getPage().reload();

		// Post-processing
		Shell.runAndWait("ffmpeg", "-y", "-i", recordingPreProcess, "-c", "copy", recordingPostProcess);

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2, recordingPostProcess).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP2.connect(webRtcEP2);

		// Playing the recording
		launchBrowser(webRtcEP2, playerEP2, null);

		// Release Media Pipeline #2
		mp2.release();

	}

	private void launchBrowser(WebRtcEndpoint webRtcEP, PlayerEndpoint playerEP, RecorderEndpoint recorderEP)
			throws InterruptedException {

		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
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
		String inRecording = (recorderEP == null) ? " in the recording" : "";

		Assert.assertTrue("Not received media (timeout waiting playing event)" + inRecording,
				getPage().waitForEvent("playing"));
		Assert.assertTrue("Color above the head must be red (FaceOverlayFilter)" + inRecording,
				getPage().similarColorAt(EXPECTED_COLOR, EXPECTED_COLOR_X, EXPECTED_COLOR_Y));
		Assert.assertTrue("Not received EOS event in player" + inRecording,
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

		if (recorderEP != null) {
			AssertMedia.assertCodecs(getDefaultOutputFile(PRE_PROCESS_SUFIX), EXPECTED_VIDEO_CODEC,
					EXPECTED_AUDIO_CODEC);
		} else {
			getPage().setThresholdTime(THRESHOLD);
			double currentTime = getPage().getCurrentTime();
			Assert.assertTrue("Error in play time in the recorded video (expected: " + PLAYTIME + " sec, real: "
					+ currentTime + " sec) " + inRecording, getPage().compare(PLAYTIME, currentTime));
		}
	}

}