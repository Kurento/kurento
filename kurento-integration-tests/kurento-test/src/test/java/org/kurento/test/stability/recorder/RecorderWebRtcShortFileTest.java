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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. Record 100 files (2 seconds) from the same
 * WebRtcEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · WebRtcEndpoint -> N WebRtcEndpoint X RecorderEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (Browser) WebRtcPeer in send-only sends media to KMS <br>
 * 2. (KMS) WebRtcEndpoint receives media and it is recorded by
 * RecorderEndpoint. <br>
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
public class RecorderWebRtcShortFileTest extends StabilityTest {

	private static final int NUM_RECORDERS = 100;
	private static final int RECORD_MS = 2000; // ms
	private static final int THRESHOLD_MS = 8000; // ms
	private static int numRecorders;

	public RecorderWebRtcShortFileTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		numRecorders = getProperty("recorder.stability.shortfiles.numrecorders",
				NUM_RECORDERS);
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testRecorderWebRtcShortFileWebm() throws Exception {
		doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
				EXTENSION_WEBM);
	}

	@Test
	public void testRecorderWebRtcShortFileMp4() throws Exception {
		doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
				EXTENSION_MP4);
	}

	public void doTest(final MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			final String extension) throws Exception {

		MediaPipeline mp = null;

		// Media Pipeline
		mp = kurentoClient.createMediaPipeline();
		final WebRtcEndpoint webRtcSender = new WebRtcEndpoint.Builder(mp)
				.build();
		final RecorderEndpoint[] recorder = new RecorderEndpoint[numRecorders];
		final String recordingFile[] = new String[numRecorders];

		// WebRTC sender negotiation
		getPage().subscribeLocalEvents("playing");
		getPage().initWebRtc(webRtcSender, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_ONLY);
		Assert.assertTrue("Not received media in sender",
				getPage().waitForEvent("playing"));

		ExecutorService executor = Executors.newFixedThreadPool(numRecorders);
		final CountDownLatch latch = new CountDownLatch(numRecorders);
		final MediaPipeline pipeline = mp;
		for (int j = 0; j < numRecorders; j++) {
			final int i = j;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						// N recorders
						recordingFile[i] = getDefaultOutputFile(
								"-receiver" + i + extension);
						recorder[i] = new RecorderEndpoint.Builder(pipeline,
								Protocol.FILE + recordingFile[i])
										.withMediaProfile(mediaProfileSpecType)
										.build();
						webRtcSender.connect(recorder[i]);

						// Start record
						recorder[i].record();

						// Wait play time
						Thread.sleep(RECORD_MS);

						// Stop record
						recorder[i].stop();

						// Guard time to stop recording
						Thread.sleep(4000);

					} catch (Throwable e) {
						log.error("Exception in receiver " + i, e);

					} finally {
						latch.countDown();
					}
				}
			});
		}

		// Wait to finish all recorders
		latch.await();

		// Assessment
		for (int j = 0; j < numRecorders; j++) {
			AssertMedia.assertCodecs(recordingFile[j], expectedVideoCodec,
					expectedAudioCodec);
			AssertMedia.assertDuration(recordingFile[j], RECORD_MS,
					THRESHOLD_MS);
		}

		// Release Media Pipeline
		if (mp != null) {
			mp.release();
		}

	}
}
