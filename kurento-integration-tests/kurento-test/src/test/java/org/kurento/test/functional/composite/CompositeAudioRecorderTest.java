/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.test.functional.composite;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Composite;
import org.kurento.client.Continuation;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.functional.recorder.BaseRecorder;

/**
 * Four synthetic videos are played by four PlayerEndpoint and mixed by a Composite. Only audio is
 * connected for three players. The resulting video is recording using a RecorderEndpoint. The
 * recorded video is played using a PlayerEndpoint.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>4xPlayerEndpoint -> Composite -> RecorderEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Media server mix the media from 4 PlayerEndpoints and it records the grid using the
 * RecorderEndpoint. Only red color (video from first player) and audio from four players.</li>
 * <li>(KMS) Media server implements a player to reproduce the video recorded in step 1.</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color of the video should be the expected in the recorded video (red)</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * </ul>
 *
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.1.1
 */

public class CompositeAudioRecorderTest extends BaseRecorder {

  private static int RECORDTIME = 20; // seconds
  private static int PLAYTIME = 20; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testCompositeRecorder() throws Exception {
    // MediaPipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    PlayerEndpoint playerRed =
        new PlayerEndpoint.Builder(mp, "http://" + getTestFilesHttpPath() + "/video/30sec/red.webm")
            .build();
    PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/green.webm").build();
    PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/blue.webm").build();

    Composite composite = new Composite.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(composite).build();
    HubPort hubPort2 = new HubPort.Builder(composite).build();
    HubPort hubPort3 = new HubPort.Builder(composite).build();

    playerRed.connect(hubPort1);
    playerGreen.connect(hubPort2, MediaType.AUDIO);
    playerBlue.connect(hubPort3, MediaType.AUDIO);

    PlayerEndpoint playerWhite = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/white.webm").build();
    HubPort hubPort4 = new HubPort.Builder(composite).build();
    playerWhite.connect(hubPort4, MediaType.AUDIO);

    HubPort hubPort5 = new HubPort.Builder(composite).build();
    String recordingFile = getDefaultOutputFile(EXTENSION_WEBM);
    RecorderEndpoint recorderEp =
        new RecorderEndpoint.Builder(mp, Protocol.FILE + recordingFile).build();
    hubPort5.connect(recorderEp);

    playerRed.play();
    playerGreen.play();
    playerBlue.play();
    playerWhite.play();

    recorderEp.record();

    Thread.sleep(RECORDTIME * 1000);

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
        recorderLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    playerRed.stop();
    playerGreen.stop();
    playerBlue.stop();
    playerWhite.stop();

    mp.release();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp2 =
        new PlayerEndpoint.Builder(mp2, Protocol.FILE + recordingFile).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEp2.connect(webRtcEp2);

    // Playing the recording
    launchBrowser(mp, webRtcEp2, playerEp2, null, EXPECTED_VIDEO_CODEC_WEBM,
        EXPECTED_AUDIO_CODEC_WEBM, recordingFile, Color.RED, 0, 0, PLAYTIME);

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }
}
