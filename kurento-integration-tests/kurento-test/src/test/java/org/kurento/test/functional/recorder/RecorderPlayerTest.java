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
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 *
 * <strong>Description</strong>: Test of a Recorder, using the stream source
 * from a PlayerEndpoint through an WebRtcEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Color of the video should be the expected</li>
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
public class RecorderPlayerTest extends FunctionalTest {

	private static final int PLAYTIME = 10; // seconds
	private static final String EXPECTED_VIDEO_CODEC_WEBM = "VP8";
	private static final String EXPECTED_VIDEO_CODEC_MP4 = "AVC";
	private static final String EXPECTED_AUDIO_CODEC_WEBM = "Vorbis";
	private static final String EXPECTED_AUDIO_CODEC_MP4 = "MPEG Audio";
	private static final Color EXPECTED_COLOR = Color.GREEN;

	public RecorderPlayerTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testRecorderPlayerWebm() throws Exception {
		doTest(MediaProfileSpecType.WEBM, EXPECTED_VIDEO_CODEC_WEBM,
				EXPECTED_AUDIO_CODEC_WEBM, ".webm");
	}

	@Test
	public void testRecorderPlayerMp4() throws Exception {
		doTest(MediaProfileSpecType.MP4, EXPECTED_VIDEO_CODEC_MP4,
				EXPECTED_AUDIO_CODEC_MP4, ".mp4");
	}

	public void doTest(MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			String extension) throws Exception {

		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/green.webm").build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();

		String recordingFile = getDefaultOutputFile(extension);

		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				Protocol.FILE + recordingFile)
						.withMediaProfile(mediaProfileSpecType).build();
		playerEP.connect(webRtcEP1);

		playerEP.connect(recorderEP);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution #1. Play the video while it is recorded
		launchBrowser(webRtcEP1, playerEP, recorderEP, expectedVideoCodec,
				expectedAudioCodec, recordingFile);

		// Wait for EOS
		Assert.assertTrue("No EOS event",
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

		// Release Media Pipeline #1
		mp.release();

		// Reloading browser
		getPage().reload();

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2,
				Protocol.FILE + recordingFile).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP2.connect(webRtcEP2);

		// Playing the recording
		launchBrowser(webRtcEP2, playerEP2, null, expectedVideoCodec,
				expectedAudioCodec, recordingFile);

		// Release Media Pipeline #2
		mp2.release();

	}

	private void launchBrowser(WebRtcEndpoint webRtcEP, PlayerEndpoint playerEP,
			RecorderEndpoint recorderEP, String expectedVideoCodec,
			String expectedAudioCodec, String recordingFile)
					throws InterruptedException {

		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);
		playerEP.play();
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

		// Assertions
		String inRecording = (recorderEP == null) ? " in the recording" : "";

		Assert.assertTrue("Not received media (timeout waiting playing event)"
				+ inRecording, getPage().waitForEvent("playing"));
		Assert.assertTrue("The color of the video should be " + EXPECTED_COLOR
				+ inRecording, getPage().similarColor(EXPECTED_COLOR));
		Assert.assertTrue("Not received EOS event in player" + inRecording,
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
		if (recorderEP != null) {
			recorderEP.stop();

			// Guard time to stop the recording
			Thread.sleep(2000);

			AssertMedia.assertCodecs(recordingFile, expectedVideoCodec,
					expectedAudioCodec);

		} else {
			double currentTime = getPage().getCurrentTime();
			Assert.assertTrue(
					"Error in play time in the recorded video (expected: "
							+ PLAYTIME + " sec, real: " + currentTime + " sec) "
							+ inRecording,
					getPage().compare(PLAYTIME, currentTime));
		}
	}

}
