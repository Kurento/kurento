/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Stability test for Recorder. Switch 100 times (each 1/2 second) with two WebRTC's. <br>
 *
 * Media Pipeline(s): <br>
 * · WebRtcEndpoint x 2 -> RecorderEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome and Firefox <br>
 *
 * Test logic: <br>
 * 1. (Browser) 2 WebRtcPeer in send-only mode sends media to KMS <br>
 * 2. (KMS) 2 WebRtcEndpoints receive media and it is recorded by 1 RecorderEndpoint. <br>
 *
 * Main assertion(s): <br>
 * · Recorded files are OK (seekable, length, content)
 *
 * Secondary assertion(s): <br>
 * -- <br>
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

    MediaPipeline mp = null;

    // Media Pipeline
    mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile = getDefaultOutputFile(extension);
    RecorderEndpoint recorderEP =
        new RecorderEndpoint.Builder(mp, Protocol.FILE + "://" + recordingFile)
            .withMediaProfile(mediaProfileSpecType).build();

    // WebRTC negotiation
    getPage(0).subscribeLocalEvents("playing");
    getPage(0).initWebRtc(webRtcEP1, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(1).subscribeLocalEvents("playing");
    getPage(1).initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

    // Start record
    recorderEP.record();

    // Switch webrtcs
    for (int i = 0; i < SWITCH_TIMES; i++) {
      if (i % 2 == 0) {
        webRtcEP1.connect(recorderEP);
      } else {
        webRtcEP2.connect(recorderEP);
      }

      Thread.sleep(SWITCH_RATE_MS);
    }

    // Stop record
    recorderEP.stop();

    // Guard time to stop recording
    Thread.sleep(4000);

    // Assessment
    Assert.assertTrue("Not received media in browser 1", getPage(0).waitForEvent("playing"));
    Assert.assertTrue("Not received media in browser 2", getPage(1).waitForEvent("playing"));

    long expectedTimeMs = SWITCH_TIMES * SWITCH_RATE_MS;
    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, expectedTimeMs, THRESHOLD_MS);

    // Release Media Pipeline
    if (mp != null) {
      mp.release();
    }

  }
}
