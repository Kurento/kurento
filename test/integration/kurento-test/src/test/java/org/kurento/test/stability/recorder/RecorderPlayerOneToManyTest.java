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

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. Player one to many recorders.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> N RecorderEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>--</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) N RecorderEndpoints recording media from 1 PlayerEndpoint.</li>
 * <li>(Browser) --</li>
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
public class RecorderPlayerOneToManyTest extends StabilityTest {

  private static final int NUM_RECORDERS = 2;
  private static final int PLAYTIME_MS = 10000; // ms
  private static final int THRESHOLD_MS = 5000; // ms
  private static int numViewers;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    numViewers = getProperty("recorder.stability.player.one2many.numrecorders", NUM_RECORDERS);
    return TestScenario.empty();
  }

  @Test
  public void testRecorderPlayerOneToManyWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderPlayerOneToManyMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(final MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, final String extension) throws Exception {

    MediaPipeline mp = null;

    // Media Pipeline
    mp = kurentoClient.createMediaPipeline();
    final PlayerEndpoint playerEp =
        new PlayerEndpoint.Builder(mp, getPlayerUrl("/video/60sec/ball.webm")).build();

    final RecorderEndpoint[] recorder = new RecorderEndpoint[numViewers];
    final String[] recordingFile = new String[numViewers];
    playerEp.play();

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
            recordingFile[i] = getRecordUrl("-recorder" + i + extension);
            recorder[i] = new RecorderEndpoint.Builder(pipeline, recordingFile[i])
                .withMediaProfile(mediaProfileSpecType).build();
            playerEp.connect(recorder[i]);

            // Start record
            recorder[i].record();

            // Wait play time
            Thread.sleep(PLAYTIME_MS);

            // Stop record
            recorder[i].stopAndWait();

            // Guard time to stop recording
            Thread.sleep(4000);

          } catch (Throwable t) {
            log.error("Exception in receiver " + i, t);
          }

          latch.countDown();
        }
      });
    }

    // Wait to finish all recordings
    latch.await();

    // Assessments
    for (int j = 0; j < numViewers; j++) {
      AssertMedia.assertCodecs(recordingFile[j], expectedVideoCodec, expectedAudioCodec);
      AssertMedia.assertDuration(recordingFile[j], PLAYTIME_MS, THRESHOLD_MS);
    }

    // Release Media Pipeline
    if (mp != null) {
      mp.release();
    }

  }
}
