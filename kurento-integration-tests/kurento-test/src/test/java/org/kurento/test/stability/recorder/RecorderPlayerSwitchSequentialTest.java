/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.stability.recorder;

import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_AUDIO_CODEC_MP4;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_AUDIO_CODEC_WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_VIDEO_CODEC_MP4;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_VIDEO_CODEC_WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXTENSION_MP4;
import static org.kurento.test.functional.recorder.BaseRecorder.EXTENSION_WEBM;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. Switch 100 times (each 1/2 second) between two
 * players. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint x 2 -> RecorderEndpoint <br>
 *
 * Browser(s): <br>
 * -- <br>
 *
 * Test logic: <br>
 * 1. (KMS) 1 RecorderEndpoint recording media from 2 PlayerEndpoint. <br>
 * 2. (Browser) -- <br>
 *
 * Main assertion(s): <br>
 * · Recorded files are OK (seekable, length, content)
 *
 * Secondary assertion(s): <br>
 * -- <br>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class RecorderPlayerSwitchSequentialTest extends StabilityTest {

	private static final int SWITCH_TIMES = 100;
	private static final int SWITCH_RATE_MS = 500; // ms
	private static final int THRESHOLD_MS = 5000; // ms

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.empty();
	}

	@Test
	public void testRecorderPlayerSwitchSequentialWebm() throws Exception {
		doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
				EXTENSION_WEBM);
	}

	@Test
	public void testRecorderPlayerSwitchSequentialMp4() throws Exception {
		doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
				EXTENSION_MP4);
	}

	public void doTest(MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			String extension) throws Exception {

		MediaPipeline mp = null;

		// Media Pipeline
		mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP1 = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/60sec/ball.webm").build();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/60sec/smpte.webm").build();

		String recordingFile = getDefaultOutputFile(extension);
		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				Protocol.FILE + recordingFile)
						.withMediaProfile(mediaProfileSpecType).build();

		// Start play and record
		playerEP1.play();
		playerEP2.play();
		recorderEP.record();

		// Switch players
		for (int i = 0; i < SWITCH_TIMES; i++) {
			if (i % 2 == 0) {
				playerEP1.connect(recorderEP);
			} else {
				playerEP2.connect(recorderEP);
			}

			Thread.sleep(SWITCH_RATE_MS);
		}

		// Stop play and record
		playerEP1.stop();
		playerEP2.stop();
		recorderEP.stop();

		// Guard time to stop recording
		Thread.sleep(4000);

		// Assessments
		long expectedTimeMs = SWITCH_TIMES * SWITCH_RATE_MS;
		AssertMedia.assertCodecs(recordingFile, expectedVideoCodec,
				expectedAudioCodec);
		AssertMedia.assertDuration(recordingFile, expectedTimeMs, THRESHOLD_MS);

		// Release Media Pipeline
		if (mp != null) {
			mp.release();
		}

	}
}
