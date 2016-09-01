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
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. Switch 100 times (each 1/2 second) between two players.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint x 2 -> RecorderEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>--</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) 1 RecorderEndpoint recording media from 2 PlayerEndpoint.</li>
 * <li>(Browser) --</li>
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
public class RecorderPlayerSwitchSequentialTest extends StabilityTest {

  private static final int SWITCH_TIMES = 100;
  private static final int SWITCH_RATE_MS = 500; // ms
  private static final int THRESHOLD_MS = 5000; // ms
  private static final int TIMEOUT = 30;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    setDeleteLogsIfSuccess(false);
    return TestScenario.empty();
  }

  @Test
  public void testRecorderPlayerSwitchSequentialWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderPlayerSwitchSequentialMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {

    final CountDownLatch recorderLatch = new CountDownLatch(1);

    MediaPipeline mp = null;

    // Media Pipeline
    mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp1 =
        new PlayerEndpoint.Builder(mp, getPlayerUrl("/video/60sec/ball.webm")).build();
    PlayerEndpoint playerEp2 =
        new PlayerEndpoint.Builder(mp, getPlayerUrl("/video/60sec/smpte.webm")).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();

    // Start play and record
    playerEp1.play();
    playerEp2.play();
    recorderEp.record();

    // Switch players
    for (int i = 0; i < SWITCH_TIMES; i++) {
      if (i % 2 == 0) {
        playerEp1.connect(recorderEp);
      } else {
        playerEp2.connect(recorderEp);
      }

      Thread.sleep(SWITCH_RATE_MS);
    }

    // Stop play and record
    playerEp1.stop();
    playerEp2.stop();
    recorderEp.stop(new Continuation<Void>() {

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
        recorderLatch.await(TIMEOUT, TimeUnit.SECONDS));

    // Assessments
    long expectedTimeMs = SWITCH_TIMES * SWITCH_RATE_MS;
    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, expectedTimeMs, THRESHOLD_MS);

    // Release Media Pipeline
    if (mp != null) {
      mp.release();
    }

  }
}
