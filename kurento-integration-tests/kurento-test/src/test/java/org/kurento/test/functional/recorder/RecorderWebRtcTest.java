/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kurento.client.MediaProfileSpecType.MP4;
import static org.kurento.client.MediaProfileSpecType.MP4_AUDIO_ONLY;
import static org.kurento.client.MediaProfileSpecType.MP4_VIDEO_ONLY;
import static org.kurento.client.MediaProfileSpecType.WEBM;
import static org.kurento.client.MediaProfileSpecType.WEBM_AUDIO_ONLY;
import static org.kurento.client.MediaProfileSpecType.WEBM_VIDEO_ONLY;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_AND_VIDEO;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_ONLY;
import static org.kurento.test.browser.WebRtcChannel.VIDEO_ONLY;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Continuation;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

import com.google.common.base.Strings;

/**
 * Test of a recorder, using the stream source from a WebRtcEndpoint. Tests recording with audio and
 * video, only audio or only video.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint & RecorderEndpoint</li> ·PlayerEndpoint -> WebRtcEndpoint
 * </li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Two media pipelines. First WebRtcEndpoint to RecorderEndpoint (recording) and then
 * PlayerEndpoint -> WebRtcEndpoint (play of the recording).</li>
 * <li>(Browser) First a WebRtcPeer in send-only sends media. Second, other WebRtcPeer in rcv-only
 * receives media</li>
 * </ul>
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
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 4.2.3
 */
public class RecorderWebRtcTest extends BaseRecorder {

  private static final int PLAYTIME = 20; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testRecorderWebRtcVideoAndAudioWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderWebRtcVideoAndAudioMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  @Test
  public void testRecorderWebRtcVideoOnlyWebm() throws Exception {
    doTest(WEBM_VIDEO_ONLY, EXPECTED_VIDEO_CODEC_WEBM, null, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderWebRtcVideoOnlyMp4() throws Exception {
    doTest(MP4_VIDEO_ONLY, EXPECTED_VIDEO_CODEC_MP4, null, EXTENSION_MP4);
  }

  @Test
  public void testRecorderWebRtcAudioOnlyWebm() throws Exception {
    doTest(WEBM_AUDIO_ONLY, null, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderWebRtcAudioOnlyMp4() throws Exception {
    doTest(MP4_AUDIO_ONLY, null, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {

    final CountDownLatch recorderLatch = new CountDownLatch(1);

    // Media Pipeline #1
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();
    webRtcEp.connect(webRtcEp);
    webRtcEp.connect(recorderEp);

    WebRtcChannel webRtcChannel = AUDIO_AND_VIDEO;
    if (Strings.isNullOrEmpty(expectedAudioCodec)) {
      webRtcChannel = VIDEO_ONLY;
    } else if (Strings.isNullOrEmpty(expectedVideoCodec)) {
      webRtcChannel = AUDIO_ONLY;
    }

    log.debug("Using webRtcChannel {}", webRtcChannel);

    // Test execution #1. WewbRTC in loopback while it is recorded
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, webRtcChannel, WebRtcMode.SEND_RCV);
    recorderEp.record();
    saveGstreamerDot(mp);

    // Wait until event playing in the remote stream
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    // Guard time to play the video
    Thread.sleep(SECONDS.toMillis(PLAYTIME));

    // Release Media Pipeline #1
    saveGstreamerDot(mp);

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

    mp.release();

    // Wait until file exists
    waitForFileExists(recordingFile);

    // Reloading browser
    getPage().reload();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp2 = new PlayerEndpoint.Builder(mp2, recordingFile).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEp2.connect(webRtcEp2);

    // Test execution #2. Playback
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp2, webRtcChannel, WebRtcMode.RCV_ONLY);
    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });
    playerEp2.play();

    // Assertions in recording
    final String messageAppend = "[played file with media pipeline]";
    final int playtime = PLAYTIME;

    Assert.assertTrue(
        "Not received media in the recording (timeout waiting playing event) " + messageAppend,
        getPage().waitForEvent("playing"));
    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(getPage().getTimeout(), SECONDS));

    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue("Error in play time in the recorded video (expected: " + playtime
        + " sec, real: " + currentTime + " sec) " + messageAppend,
        getPage().compare(playtime, currentTime));

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);

    AssertMedia.assertGeneralDuration(recordingFile, SECONDS.toMillis(playtime),
        SECONDS.toMillis(getPage().getThresholdTime()));

    if (webRtcChannel == AUDIO_AND_VIDEO || webRtcChannel == AUDIO_ONLY) {
      AssertMedia.assertAudioDuration(recordingFile, SECONDS.toMillis(playtime),
          SECONDS.toMillis(getPage().getThresholdTime()));
    }

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }

}
