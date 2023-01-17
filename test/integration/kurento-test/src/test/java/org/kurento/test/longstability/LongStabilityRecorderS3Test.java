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

package org.kurento.test.longstability;

import static org.kurento.test.config.TestConfiguration.TEST_DURATION_PROPERTY;
import static org.kurento.test.functional.recorder.BaseRecorder.EXPECTED_AUDIO_CODEC_WEBM;
import static org.kurento.test.functional.recorder.BaseRecorder.EXTENSION_WEBM;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Continuation;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
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
 * Stability test for Recorder. Record for 8 hours.
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
 * <li>(KMS) WebRtcEndpoint receives media and it is recorded by RecorderEndpoint (Only Audio).</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Recorded files are OK (length)</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */
public class LongStabilityRecorderS3Test extends StabilityTest {

  private static final int THRESHOLD_MS = 5000; // ms
  private String msgError = "";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    return TestScenario.localChrome();
  }

  @Test
  public void testLongStabilityRecorderS3Webm() throws Exception {
    doTest(MediaProfileSpecType.WEBM_AUDIO_ONLY, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  public void doTest(final MediaProfileSpecType mediaProfileSpecType, String expectedAudioCodec,
      final String extension) throws Exception {

    long testDurationMillis =
        PropertiesManager.getProperty(TEST_DURATION_PROPERTY, DEFAULT_TEST_DURATION);

    MediaPipeline mp = kurentoClient.createMediaPipeline();

    final CountDownLatch errorPipelinelatch = new CountDownLatch(1);

    mp.addErrorListener(new EventListener<ErrorEvent>() {

      @Override
      public void onEvent(ErrorEvent event) {
        msgError = "Description:" + event.getDescription() + "; Error code:" + event.getType();
        log.error(msgError);
        errorPipelinelatch.countDown();
      }
    });
    final WebRtcEndpoint webRtcSender = new WebRtcEndpoint.Builder(mp).build();

    // WebRTC sender negotiation
    getPage().subscribeLocalEvents("playing");
    getPage().initWebRtc(webRtcSender, WebRtcChannel.AUDIO_ONLY, WebRtcMode.SEND_ONLY);
    Assert.assertTrue("Not received media in sender webrtc", getPage().waitForEvent("playing"));

    // Recorder
    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorder = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();
    webRtcSender.connect(recorder);

    // Start recorder
    recorder.record();

    // Wait recording time
    Thread.sleep(testDurationMillis);

    // Stop recorder
    final CountDownLatch recorderLatch = new CountDownLatch(1);
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

    // Release Media Pipeline
    Assert.assertTrue("Not stop properly",
        recorderLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
    if (mp != null) {
      mp.release();
    }

    Assert.assertTrue(msgError, errorPipelinelatch.getCount() == 1);

    waitForFileExists(recordingFile);

    // Assessments
    AssertMedia.assertDuration(recordingFile, testDurationMillis, THRESHOLD_MS);

  }
}
