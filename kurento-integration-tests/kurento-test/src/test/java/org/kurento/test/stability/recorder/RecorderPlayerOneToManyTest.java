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
import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_AUDIO_CODEC_MP4;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_AUDIO_CODEC_WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_VIDEO_CODEC_MP4;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_VIDEO_CODEC_WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXTENSION_MP4;
import static org.kurento.test.functional.recorder.BaseRecorder.EXTENSION_WEBM;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * <strong>Description</strong>: Stability test for Recorder. Player one to many
 * recorders.<br>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> N RecorderEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Recorded files are OK (seekable, length, content)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class RecorderPlayerOneToManyTest extends StabilityTest {

	private static final int NUM_RECORDERS = 5;
	private static final int PLAYTIME_MS = 10000; // ms
	private static final int THRESHOLD_MS = 5000; // ms
	private static int numViewers;

	public RecorderPlayerOneToManyTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		numViewers = getProperty(
				"recorder.stability.player.one2many.numrecorders",
				NUM_RECORDERS);
		return TestScenario.noBrowsers();
	}

	@Test
	public void testRecorderPlayerOneToManyWebm() throws Exception {
		doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
				EXTENSION_WEBM);
	}

	@Test
	public void testRecorderPlayerOneToManyMp4() throws Exception {
		doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
				EXTENSION_MP4);
	}

	public void doTest(final MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			final String extension) throws Exception {

		MediaPipeline mp = null;

		// Media Pipeline
		mp = kurentoClient.createMediaPipeline();
		final PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/60sec/ball.webm").build();
		final RecorderEndpoint[] recorder = new RecorderEndpoint[numViewers];
		final String recordingFile[] = new String[numViewers];
		playerEP.play();

		ExecutorService executor = Executors.newFixedThreadPool(numViewers);
		final CountDownLatch latch = new CountDownLatch(numViewers);
		final MediaPipeline pipeline = mp;
		for (int j = 0; j < numViewers; j++) {
			final int i = j;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						// N recorders
						recordingFile[i] = getDefaultOutputFile(
								"-recorder" + i + extension);
						recorder[i] = new RecorderEndpoint.Builder(pipeline,
								Protocol.FILE + recordingFile[i])
										.withMediaProfile(mediaProfileSpecType)
										.build();
						playerEP.connect(recorder[i]);

						// Start record
						recorder[i].record();

						// Wait play time
						Thread.sleep(PLAYTIME_MS);

					} catch (Throwable t) {
						log.error("Exception in receiver " + i, t);
					}

					latch.countDown();
				}
			});
		}

		// Wait to finish all recordings
		latch.await();

		// Stop recorders
		final CountDownLatch stopLatch = new CountDownLatch(numViewers);
		for (int j = 0; j < numViewers; j++) {
			final int i = j;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						// Stop record
						recorder[i].stop();

					} catch (Throwable t) {
						log.error("Exception in receiver " + i, t);
					}
					stopLatch.countDown();
				}
			});
		}

		// Wait to finish all stops
		stopLatch.await();

		// Assessments
		for (int j = 0; j < numViewers; j++) {
			AssertMedia.assertCodecs(recordingFile[j], expectedVideoCodec,
					expectedAudioCodec);
			AssertMedia.assertDuration(recordingFile[j], PLAYTIME_MS,
					THRESHOLD_MS);
		}

		// Release Media Pipeline
		if (mp != null) {
			mp.release();
		}

	}
}
