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
import java.util.concurrent.TimeUnit;

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
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. WebRTC one to many with recorders.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> N x (WebRtcEndpoint -> RecorderEndpoint)</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>N x Chrome</li>
 * <li>N x Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(Browser) WebRtcPeer in send-only sends media to KMS</li>
 * <li>(KMS) N WebRtcEndpoints receives media and it is recorded by N RecorderEndpoints.</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Recorded files are OK (seekable, length, content)
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 * <strong>Description</strong>: Stability test for Recorder. WebRTC one to many with recorders.
 * </p>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> N WebRtcEndpoint X RecorderEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Recorded files are OK (seekable, length, content)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class RecorderWebRtcOneToManyTest extends StabilityTest {

  private static final int NUM_VIEWERS = 3;
  private static final int PLAYTIME_MS = 10000; // ms
  private static final int THRESHOLD_MS = 8000; // ms
  private static int numViewers;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    numViewers = getProperty("recorder.stability.one2many.numviewers", NUM_VIEWERS);
    return TestScenario.localChromesAndFirefoxs(numViewers + 1);
  }

  @Test
  public void testRecorderWebRtcOneToManyWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderWebRtcOneToManyMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(final MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, final String extension) throws Exception {

    MediaPipeline mp = null;

    // Media Pipeline
    mp = kurentoClient.createMediaPipeline();
    final WebRtcEndpoint webRtcSender = new WebRtcEndpoint.Builder(mp).build();
    final WebRtcEndpoint[] webRtcReceiver = new WebRtcEndpoint[numViewers];
    final RecorderEndpoint[] recorder = new RecorderEndpoint[numViewers];
    final String[] recordingFile = new String[numViewers];

    // WebRTC sender negotiation
    getPage(0).subscribeLocalEvents("playing");
    getPage(0).initWebRtc(webRtcSender, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    Assert.assertTrue("Not received media in sender", getPage(0).waitForEvent("playing"));

    ExecutorService executor = Executors.newFixedThreadPool(numViewers);
    final CountDownLatch latch = new CountDownLatch(numViewers);
    final MediaPipeline pipeline = mp;
    for (int j = 1; j <= numViewers; j++) {
      final int i = j;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          // N Receiver WebRTC and Recorder
          webRtcReceiver[i - 1] = new WebRtcEndpoint.Builder(pipeline).build();
          recordingFile[i - 1] = getRecordUrl("-receiver" + i + extension);
          recorder[i - 1] = new RecorderEndpoint.Builder(pipeline, recordingFile[i - 1])
              .withMediaProfile(mediaProfileSpecType).build();

          webRtcSender.connect(webRtcReceiver[i - 1]);
          webRtcSender.connect(recorder[i - 1]);

          try {
            // WebRTC receiver negotiation
            getPage(i).subscribeEvents("playing");
            getPage(i).initWebRtc(webRtcReceiver[i - 1], WebRtcChannel.AUDIO_AND_VIDEO,
                WebRtcMode.RCV_ONLY);
            Assert.assertTrue("Not received media in receiver " + i,
                getPage(i).waitForEvent("playing"));

            // Start record
            recorder[i - 1].record();

            // Wait play time
            Thread.sleep(PLAYTIME_MS);

            // Stop record
            recorder[i - 1].stopAndWait();

            // Guard time to stop recording
            Thread.sleep(4000);

          } catch (InterruptedException e) {
            log.error("InterruptedException in receiver " + i, e);
          }

          latch.countDown();
        }
      });
    }

    // Wait to finish all receivers
    latch.await(getPage(0).getTimeout(), TimeUnit.SECONDS);

    // Assessments
    for (int j = 1; j <= numViewers; j++) {
      AssertMedia.assertCodecs(recordingFile[j - 1], expectedVideoCodec, expectedAudioCodec);
      AssertMedia.assertDuration(recordingFile[j - 1], PLAYTIME_MS, THRESHOLD_MS);
    }

    // Release Media Pipeline
    if (mp != null) {
      mp.release();
    }

  }
}
