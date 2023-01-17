/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.test.stability.recorder;

import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.WEBM;
import static org.kurento.test.config.TestConfiguration.TEST_DURATION_PROPERTY;
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
import org.kurento.commons.PropertiesManager;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. Create three connections WebRTC and record it.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> RecorderEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(Browser) WebRtcPeer in send-only sends media to KMS</li>
 * <li>(KMS) WebRtcEndpoint receives media and it is recorded by RecorderEndpoint.</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Recorded files are OK (seekable, length, content)</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public class RecorderThreeWebRtcSimultaneous extends StabilityTest {

  private static final int RECORD_MS = 2 * 60 * 1000; // ms
  private static final int THRESHOLD_MS = 5000; // ms
  private static final int NUM_BROWSERS = 3;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    return TestScenario.localChromes(NUM_BROWSERS);
  }

  @Test
  public void testRecorderWebRtcSimultaneousWebm() throws Exception {
    long testDurationMillis =
        PropertiesManager.getProperty(TEST_DURATION_PROPERTY, DEFAULT_TEST_DURATION);

    endTestTime = System.currentTimeMillis() + testDurationMillis;
    while (!isTimeToFinishTest()) {
      doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
    }
  }

  @Test
  public void testRecorderWebRtcSimultaneousMp4() throws Exception {
    long testDurationMillis =
        PropertiesManager.getProperty(TEST_DURATION_PROPERTY, DEFAULT_TEST_DURATION);

    endTestTime = System.currentTimeMillis() + testDurationMillis;
    while (!isTimeToFinishTest()) {
      doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
    }
  }

  public void doTest(final MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, final String extension) throws Exception {

    MediaPipeline mp = null;

    // Media Pipeline
    mp = kurentoClient.createMediaPipeline();

    final WebRtcEndpoint[] webRtcSender = new WebRtcEndpoint[NUM_BROWSERS];
    final RecorderEndpoint[] recorder = new RecorderEndpoint[NUM_BROWSERS];
    final String[] recordingFile = new String[NUM_BROWSERS];

    ExecutorService executor = Executors.newFixedThreadPool(NUM_BROWSERS);
    final CountDownLatch latch = new CountDownLatch(NUM_BROWSERS);
    final MediaPipeline pipeline = mp;
    for (int j = 0; j < NUM_BROWSERS; j++) {
      final int i = j;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            // N viewer
            webRtcSender[i] = new WebRtcEndpoint.Builder(pipeline).build();
            // N recorders
            recordingFile[i] = getRecordUrl("-receiver" + i + extension);
            recorder[i] = new RecorderEndpoint.Builder(pipeline, recordingFile[i])
                .withMediaProfile(mediaProfileSpecType).build();

            // WebRTC receiver negotiation
            getPage(i).subscribeLocalEvents("playing");
            getPage(i).initWebRtc(webRtcSender[i], WebRtcChannel.AUDIO_AND_VIDEO,
                WebRtcMode.SEND_ONLY);
            Assert.assertTrue("Not received media in sender" + i,
                getPage(i).waitForEvent("playing"));

            webRtcSender[i].connect(recorder[i]);

            // Start record
            recorder[i].record();

            // Wait play time
            Thread.sleep(RECORD_MS);

            // Stop record
            recorder[i].stopAndWait();

            // Guard time to stop recording
            Thread.sleep(4000);
            getPage(i).reload();
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
    for (int j = 0; j < NUM_BROWSERS; j++) {
      AssertMedia.assertCodecs(recordingFile[j], expectedVideoCodec, expectedAudioCodec);
      AssertMedia.assertDuration(recordingFile[j], RECORD_MS, THRESHOLD_MS);
    }

    // Release Media Pipeline
    if (mp != null) {
      mp.release();
    }
  }
}
