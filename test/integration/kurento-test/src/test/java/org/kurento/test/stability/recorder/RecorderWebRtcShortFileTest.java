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
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Continuation;
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
 * Stability test for Recorder. Record one file each (2 seconds) from the same WebRtcEndpoint
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> N RecorderEndpoint</li>
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
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class RecorderWebRtcShortFileTest extends StabilityTest {

  private static final int RECORD_MS = 4000; // ms
  private static final int THRESHOLD_MS = 8000; // ms

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testRecorderWebRtcShortFileWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderWebRtcShortFileMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(final MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, final String extension) throws Exception {

    final CountDownLatch recorderLatch = new CountDownLatch(1);

    long testDurationMillis =
        PropertiesManager.getProperty(TEST_DURATION_PROPERTY, DEFAULT_TEST_DURATION);

    endTestTime = System.currentTimeMillis() + testDurationMillis;

    MediaPipeline pipeline = kurentoClient.createMediaPipeline();
    final WebRtcEndpoint webRtcSender = new WebRtcEndpoint.Builder(pipeline).build();
    final String recordingFile = getRecordUrl(extension);
    final RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();

    // WebRTC sender negotiation
    getPage().subscribeLocalEvents("playing");
    getPage().initWebRtc(webRtcSender, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    Assert.assertTrue("Not received media in sender", getPage().waitForEvent("playing"));

    webRtcSender.connect(recorder);

    while (!isTimeToFinishTest()) {
      // Start record
      recorder.record();
      // Wait play time
      Thread.sleep(RECORD_MS);
      // Pause record
      recorder.pause();
      Thread.sleep(RECORD_MS);
    }

    // Stop record
    recorder.stopAndWait(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        recorderLatch.countDown();
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        recorderLatch.countDown();
      }
    });

    Assert.assertTrue("Not stop properly",
        recorderLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, testDurationMillis / 2,
        (testDurationMillis / 2) + THRESHOLD_MS);

    // Release Media Pipeline
    if (pipeline != null) {
      pipeline.release();
    }
  }
}
