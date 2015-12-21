/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.test.functional.recorder;

import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.WEBM;

import java.awt.Color;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of a Recorder, using the stream source from a PlayerEndpoint with FaceOverlayFilter through
 * an WebRtcEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> FaceOverlayFilter -> RecorderEndpoint & WebRtcEndpoint <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) Two media pipelines. First PlayerEndpoint to RecorderEndpoint (recording) and then
 * PlayerEndpoint -> WebRtcEndpoint (play of the recording). <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag (in the recording) <br>
 * · The color of the received video should be as expected (in the recording) <br>
 * · EOS event should arrive to player (in the recording) <br>
 * · Play time in remote video should be as expected (in the recording) <br>
 * · Codecs should be as expected (in the recording) <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag (in the playing) <br>
 * · The color of the received video should be as expected (in the playing) <br>
 * · EOS event should arrive to player (in the playing) <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderFaceOverlayTest extends BaseRecorder {

  private static final int PLAYTIME = 30; // seconds
  private static final int THRESHOLD = 20; // seconds
  private static final Color EXPECTED_COLOR = Color.RED;
  private static final int EXPECTED_COLOR_X = 420;
  private static final int EXPECTED_COLOR_Y = 45;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testRecorderFaceOverlayWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderFaceOverlayMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {

    // Media Pipeline #1
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
        "http://files.kurento.org/video/filter/fiwarecut.mp4").build();
    WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile = getDefaultOutputFile(extension);

    RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
        Protocol.FILE + "://" + recordingFile).withMediaProfile(mediaProfileSpecType).build();
    FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp).build();
    filter.setOverlayedImage("http://files.kurento.org/img/red-square.png", -0.2F, -1.2F, 1.6F,
        1.6F);

    playerEP.connect(filter);
    filter.connect(webRtcEP1);
    filter.connect(recorderEP);

    // Test execution #1. Play and record
    getPage().setThresholdTime(THRESHOLD);
    launchBrowser(mp, webRtcEP1, playerEP, recorderEP, expectedVideoCodec, expectedAudioCodec,
        recordingFile, EXPECTED_COLOR, EXPECTED_COLOR_X, EXPECTED_COLOR_Y, PLAYTIME);

    // Release Media Pipeline #1
    mp.release();

    // Reloading browser
    getPage().reload();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2,
        Protocol.FILE + "://" + recordingFile).build();
    WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEP2.connect(webRtcEP2);

    // Playing the recording
    launchBrowser(mp, webRtcEP2, playerEP2, null, expectedVideoCodec, expectedAudioCodec,
        recordingFile, EXPECTED_COLOR, EXPECTED_COLOR_X, EXPECTED_COLOR_Y, PLAYTIME);

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }

}
