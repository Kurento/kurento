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

package org.kurento.test.functional.recorder;

import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.WEBM;

import java.awt.Color;
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
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Test of a Recorder switching sources from WebRtc Endpoint
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>2 x Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * (KMS) Two media pipelines. First WebRtcEndpoint to RecorderEndpoint (recording) and then
 * PlayerEndpoint -> WebRtcEndpoint (play of the recording).</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag (in the recording)</li>
 * <li>The color of the received video should be as expected (in the recording)</li>
 * <li>EOS event should arrive to player (in the recording)</li>
 * <li>Play time in remote video should be as expected (in the recording)</li>
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag (in the playing)</li>
 * <li>The color of the received video should be as expected (in the playing)</li>
 * <li>EOS event should arrive to player (in the playing)</li>
 * </ul>
 *
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.1.1
 */
public class RecorderSwitchPlayerWebRtcTest extends BaseRecorder {

  private static final int PLAYTIME = 10; // seconds
  private static final int NUM_SWAPS = 6;
  private static final Color[] EXPECTED_COLORS = { Color.RED, Color.GREEN };

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromes(2);
  }

  @Test
  public void testRecorderSwitchPlayerWebRtcWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchPlayerWebRtcMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {
    // Media Pipeline #1
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    PlayerEndpoint playerRed =
        new PlayerEndpoint.Builder(mp, getPlayerUrl("/video/10sec/red.webm")).build();
    playerRed.connect(webRtcEp);

    // Test execution
    getPage(0).subscribeLocalEvents("playing");
    getPage(0).initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    playerRed.play();

    // red
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(0).waitForEvent("playing"));

    PlayerEndpoint playerGreen =
        new PlayerEndpoint.Builder(mp, getPlayerUrl("/video/10sec/green.webm")).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();

    playerGreen.play();
    recorderEp.record();
    for (int i = 0; i < NUM_SWAPS; i++) {
      if (i % 2 == 0) {
        playerRed.connect(recorderEp);
      } else {
        playerGreen.connect(recorderEp);
      }

      Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / NUM_SWAPS);
    }

    // Release Media Pipeline #1
    saveGstreamerDot(mp);

    final CountDownLatch recorderLatch = new CountDownLatch(1);
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

    Assert.assertTrue("Not stop properly",
        recorderLatch.await(getPage(0).getTimeout(), TimeUnit.SECONDS));
    mp.release();

    // Reloading browser
    getPage(0).close();

    checkRecordingFile(recordingFile, "browser1", EXPECTED_COLORS, PLAYTIME, expectedVideoCodec,
        expectedAudioCodec);
    success = true;
  }
}
