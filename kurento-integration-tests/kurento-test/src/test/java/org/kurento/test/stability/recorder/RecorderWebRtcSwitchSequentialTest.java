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
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. Switch 100 times (each 1/2 second) with two WebRTC's.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint x 2 -> RecorderEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome and Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(Browser) 2 WebRtcPeer in send-only mode sends media to KMS</li>
 * <li>(KMS) 2 WebRtcEndpoints receive media and it is recorded by 1 RecorderEndpoint.</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Recorded files are OK (seekable, length, content)
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class RecorderWebRtcSwitchSequentialTest extends StabilityTest {

  private static final int SWITCH_TIMES = 100;
  private static final int SWITCH_RATE_MS = 500; // ms
  private static final int THRESHOLD_MS = 5000; // ms

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    return TestScenario.localChromePlusFirefox();
  }

  @Test
  public void testRecorderWebRtcSwitchSequentialWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderWebRtcSwitchSequentialMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {

    final CountDownLatch recorderLatch = new CountDownLatch(1);

    MediaPipeline mp = null;

    // Media Pipeline
    mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEp1 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp).build();

    // WebRTC negotiation
    getPage(0).subscribeLocalEvents("playing");
    getPage(0).initWebRtc(webRtcEp1, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(1).subscribeLocalEvents("playing");
    getPage(1).initWebRtc(webRtcEp2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

    // Start record
    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();
    recorderEp.record();

    // Switch webrtcs
    for (int i = 0; i < SWITCH_TIMES; i++) {
      if (i % 2 == 0) {
        webRtcEp1.connect(recorderEp);
      } else {
        webRtcEp2.connect(recorderEp);
      }

      Thread.sleep(SWITCH_RATE_MS);
    }

    // Stop record
    recorderEp.stopAndWait(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        recorderLatch.countDown();
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        recorderLatch.countDown();
      }
    });

    // Assessment
    Assert.assertTrue("Not received media in browser 1", getPage(0).waitForEvent("playing"));
    Assert.assertTrue("Not received media in browser 2", getPage(1).waitForEvent("playing"));

    Assert.assertTrue("Not stop properly",
        recorderLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    long expectedTimeMs = SWITCH_TIMES * SWITCH_RATE_MS;
    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, expectedTimeMs, THRESHOLD_MS);

    // Release Media Pipeline
    if (mp != null) {
      mp.release();
    }

  }
}
