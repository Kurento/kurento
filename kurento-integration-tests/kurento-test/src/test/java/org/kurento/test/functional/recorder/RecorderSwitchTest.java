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

import org.kurento.test.base.FunctionalTest;
import org.kurento.test.config.TestScenario;

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
 * <li>Color of the video should be the expected</li>
 * <li>Media should be received in the video tag (in the recording)</li>
 * <li>Color of the video should be the expected (in the recording)</li>
 * <li>Ended event should arrive to player (in the recording)</li>
 * <li>Play time should be the expected (in the recording)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderSwitchTest extends FunctionalTest {

	public RecorderSwitchTest(TestScenario testScenario) {
		super(testScenario);
	}
	
	// FIXME activate

//	private static final int PLAYTIME = 20; // seconds
//	private static final int TIMEOUT = 120; // seconds
//	private static final int N_PLAYER = 3;
//	private static final String EXPECTED_VIDEO_CODEC = "VP8";
//	private static final String EXPECTED_AUDIO_CODEC = "Vorbis";
//	private static final String PRE_PROCESS_SUFIX = "-preprocess.webm";
//	private static final Color[] EXPECTED_COLORS = { Color.RED, Color.GREEN,
//			Color.BLUE };
//
//	@Test
//	public void testRecorderSwitchChrome() throws Exception {
//		doTest(BrowserType.CHROME);
//	}
//
//	@Test
//	public void testRecorderSwitchFirefox() throws Exception {
//		doTest(BrowserType.FIREFOX);
//	}
//
//	public void doTest(BrowserType browserType) throws Exception {
//		// Media Pipeline #1
//		MediaPipeline mp = kurentoClient.createMediaPipeline();
//		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/10sec/red.webm").build();
//		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/10sec/green.webm").build();
//		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/10sec/blue.webm").build();
//		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
//
//		final String recordingPreProcess = FILE_SCHEMA
//				+ getDefaultOutputFile(PRE_PROCESS_SUFIX);
//		final String recordingPostProcess = FILE_SCHEMA
//				+ getDefaultFileForRecording();
//		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
//				recordingPreProcess).build();
//
//		try (BrowserClient browser = new BrowserClient.Builder()
//				.browserType(browserType).client(Client.WEBRTC).build()) {
//			browser.setTimeout(TIMEOUT);
//			browser.subscribeEvents("playing");
//			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
//					WebRtcMode.RCV_ONLY);
//
//			// red
//			playerRed.connect(webRtcEP);
//			playerRed.connect(recorderEP);
//			playerRed.play();
//			recorderEP.record();
//
//			Assert.assertTrue(
//					"Not received media (timeout waiting playing event)",
//					browser.waitForEvent("playing"));
//			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);
//
//			// green
//			playerGreen.connect(webRtcEP);
//			playerGreen.connect(recorderEP);
//			playerGreen.play();
//			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);
//
//			// blue
//			playerBlue.connect(webRtcEP);
//			playerBlue.connect(recorderEP);
//			playerBlue.play();
//			Thread.sleep(PLAYTIME * 1000 / N_PLAYER);
//
//			// Assertions
//			AssertMedia.assertCodecs(getDefaultOutputFile(PRE_PROCESS_SUFIX),
//					EXPECTED_VIDEO_CODEC, EXPECTED_AUDIO_CODEC);
//		}
//
//		// Release Media Pipeline #1
//		recorderEP.stop();
//		mp.release();
//
//		// Post-processing
//		Shell.runAndWait("ffmpeg", "-i", recordingPreProcess, "-c", "copy",
//				recordingPostProcess);
//
//		// Play the recording
//		playFileAsLocal(browserType, recordingPostProcess, PLAYTIME,
//				EXPECTED_COLORS);
//
//		// Uncomment this line to play the recording with a new pipeline
//		// playFileWithPipeline(browserType, recordingPostProcess, PLAYTIME,
//		// EXPECTED_COLORS);
//	}
}
