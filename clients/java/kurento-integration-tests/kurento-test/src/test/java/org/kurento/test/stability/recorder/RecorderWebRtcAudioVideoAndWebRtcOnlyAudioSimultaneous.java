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

import static org.kurento.client.MediaProfileSpecType.WEBM;
import static org.kurento.test.config.TestConfiguration.TEST_DURATION_PROPERTY;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_AUDIO_CODEC_WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_VIDEO_CODEC_WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXTENSION_WEBM;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
 * Stability test for Recorder. Create two pipelines, and each of them have 2 connections WebRTC and
 * record it. One of them, only records the Audio and the another records Audio and Video
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>Pipeline 1: WebRtcEndpoint -> WebRtcEndpoint -> RecorderEndpoint</li>
 * <li>Pipeline 2: WebRtcEndpoint -> WebRtcEndpoint -> RecorderEndpoint</li>
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
 * @since 6.5.1
 */
public class RecorderWebRtcAudioVideoAndWebRtcOnlyAudioSimultaneous extends StabilityTest {

  private static final int RECORD_MS = 1 * 60 * 1000; // ms
  private static final int THRESHOLD_MS = 10000; // ms
  private static final int NUM_BROWSERS = 8;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    return TestScenario.localChromes(NUM_BROWSERS);
  }

  @Test
  public void testRecorderWebRtcAudioVideoAndWebRtcOnlyAudioSimultaneousWebm() throws Exception {
    long testDurationMillis =
        PropertiesManager.getProperty(TEST_DURATION_PROPERTY, DEFAULT_TEST_DURATION);

    endTestTime = System.currentTimeMillis() + testDurationMillis;
    while (!isTimeToFinishTest()) {
      doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
    }
  }

  public void doTest(final MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, final String extension) throws Exception {

    ExecutorService executor = Executors.newFixedThreadPool(2);
    final CountDownLatch latch = new CountDownLatch(2);
    for (int j = 0; j < 2; j++) {
      final int i = j * 4;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            final MediaPipeline pipeline = kurentoClient.createMediaPipeline();

            WebRtcEndpoint webRtcSender0 = new WebRtcEndpoint.Builder(pipeline).build();
            WebRtcEndpoint webRtcSender1 = new WebRtcEndpoint.Builder(pipeline).build();
            WebRtcEndpoint webRtcSender2 = new WebRtcEndpoint.Builder(pipeline).build();
            WebRtcEndpoint webRtcSender3 = new WebRtcEndpoint.Builder(pipeline).build();

            String recordingFile0 = getRecordUrl("-receiver" + i + "-" + 0 + extension);
            String recordingFile1 = getRecordUrl("-receiver" + i + "-" + 1 + extension);
            RecorderEndpoint recorder0 = new RecorderEndpoint.Builder(pipeline, recordingFile0)
                .withMediaProfile(mediaProfileSpecType).build();
            RecorderEndpoint recorder1 = new RecorderEndpoint.Builder(pipeline, recordingFile1)
                .withMediaProfile(MediaProfileSpecType.WEBM_AUDIO_ONLY).build();

            getPage(i).subscribeLocalEvents("playing");

            getPage(i).initWebRtc(webRtcSender0, WebRtcChannel.AUDIO_AND_VIDEO,
                WebRtcMode.SEND_RCV);

            getPage(i + 1).subscribeLocalEvents("playing");

            getPage(i + 1).initWebRtc(webRtcSender1, WebRtcChannel.AUDIO_AND_VIDEO,
                WebRtcMode.SEND_RCV);

            getPage(i + 2).subscribeLocalEvents("playing");

            getPage(i + 2).initWebRtc(webRtcSender2, WebRtcChannel.AUDIO_AND_VIDEO,
                WebRtcMode.SEND_RCV);

            getPage(i + 3).subscribeLocalEvents("playing");

            getPage(i + 3).initWebRtc(webRtcSender3, WebRtcChannel.AUDIO_AND_VIDEO,
                WebRtcMode.SEND_RCV);

            Assert.assertTrue("Not received media in sender" + 0,
                getPage(i).waitForEvent("playing"));

            Assert.assertTrue("Not received media in sender" + 1,
                getPage(i + 1).waitForEvent("playing"));

            Assert.assertTrue("Not received media in sender" + 2,
                getPage(i + 2).waitForEvent("playing"));

            Assert.assertTrue("Not received media in sender" + 3,
                getPage(i + 3).waitForEvent("playing"));

            webRtcSender0.connect(webRtcSender1);
            webRtcSender2.connect(webRtcSender3);

            webRtcSender0.connect(recorder0);

            webRtcSender2.connect(recorder1);

            // Start record
            recorder0.record();
            recorder1.record();

            // Wait play time
            Thread.sleep(RECORD_MS);

            // Stop record
            recorder0.stopAndWait();
            recorder1.stopAndWait();

            // Guard time to stop recording
            Thread.sleep(5000);
            log.debug(".....Next...");
            pipeline.release();
          } catch (Throwable e) {
            log.error("Exception in receiver ", e);

          } finally {
            latch.countDown();
          }
        }
      });
    }

    // Wait to finish all recorders
    latch.await(RECORD_MS * 2, TimeUnit.SECONDS);

    // Assessment
    String recordingFile0 = getRecordUrl("-receiver" + 0 + "-" + 0 + extension);
    AssertMedia.assertCodecs(recordingFile0, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile0, RECORD_MS, THRESHOLD_MS);
    String recordingFile1 = getRecordUrl("-receiver" + 0 + "-" + 1 + extension);
    AssertMedia.assertDuration(recordingFile1, RECORD_MS, THRESHOLD_MS);

    recordingFile0 = getRecordUrl("-receiver" + 4 + "-" + 0 + extension);
    AssertMedia.assertCodecs(recordingFile0, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile0, RECORD_MS, THRESHOLD_MS);
    recordingFile1 = getRecordUrl("-receiver" + 4 + "-" + 1 + extension);
    AssertMedia.assertDuration(recordingFile1, RECORD_MS, THRESHOLD_MS);
  }
}
