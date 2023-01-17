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
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PassThrough;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Test of a Recorder switching sources from PlayerEndpoint with a PassThrough.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> WebRtcEndpoint & PassThrough; PassThrough -> RecorderEndpoint)</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * (KMS) One media pipeline. PassThrough to RecorderEndpoint (recording) and then PlayerEndpoint ->
 * WebRtcEndpoint && PlayerEndpoint -> PassThrough (play of the recording).</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * <li>While recording the player will change format or size of the video</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag (in the recording)</li>
 * <li>EOS event should arrive to player (in the recording)</li>
 * <li>Play time in remote video should be as expected (in the recording)</li>
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */
public class RecorderSwitchPlayerWithPassThroughTest extends BaseRecorder {

  private static final int PLAYTIME = 20; // seconds
  private String msgError = "";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testRecorderSwitchSameFormatPlayerWithPassThroughWebm() throws Exception {
    doTestSameFormats(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchSameFormatPlayerWithPassThroughMp4() throws Exception {
    doTestSameFormats(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  @Test
  public void testRecorderSwitchDifferentFormatPlayerWithPassThroughWebm() throws Exception {
    doTestDifferentFormats(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
        EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchDifferentFormatPlayerWithPassThroughMp4() throws Exception {
    doTestDifferentFormats(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  @Test
  public void testRecorderSwitchFrameRateDifferentPlayerWithPassThroughWebm() throws Exception {
    doTestFrameRateDifferent(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
        EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchFrameRateDifferentPlayerWithPassThroughMp4() throws Exception {
    doTestFrameRateDifferent(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
        EXTENSION_MP4);
  }

  @Test
  public void testRecorderSwitchFrameSizeDifferentPlayerWithPassThroughWebm() throws Exception {
    doTestFrameSizeDifferent(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
        EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchFrameSizeDifferentPlayerWithPassThroughMp4() throws Exception {
    doTestFrameSizeDifferent(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
        EXTENSION_MP4);
  }

  @Test
  public void testRecorderSwitchFrameRateFrameSizeDifferentPlayerWithPassThroughWebm()
      throws Exception {
    doTestFrameRateAndFrameSizeDifferent(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
        EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchFrameRateFrameSizeDifferentPlayerWithPassThroughMp4()
      throws Exception {
    doTestFrameRateAndFrameSizeDifferent(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4,
        EXTENSION_MP4);
  }

  public void doTestSameFormats(MediaProfileSpecType mediaProfileSpecType,
      String expectedVideoCodec, String expectedAudioCodec, String extension) throws Exception {
    String[] mediaUrls = { getPlayerUrl("/video/10sec/red.webm"),
        getPlayerUrl("/video/10sec/green.webm"), getPlayerUrl("/video/10sec/red.webm") };
    Color[] expectedColors = { Color.RED, Color.GREEN, Color.RED };

    doTest(mediaProfileSpecType, expectedVideoCodec, expectedAudioCodec, extension, mediaUrls,
        expectedColors);
  }

  public void doTestDifferentFormats(MediaProfileSpecType mediaProfileSpecType,
      String expectedVideoCodec, String expectedAudioCodec, String extension) throws Exception {
    String[] mediaUrls = { getPlayerUrl("/video/10sec/ball.mkv"),
        getPlayerUrl("/video/10sec/white.webm"), getPlayerUrl("/video/10sec/ball.mkv") };
    Color[] expectedColors = { Color.BLACK, Color.WHITE, Color.BLACK };

    doTest(mediaProfileSpecType, expectedVideoCodec, expectedAudioCodec, extension, mediaUrls,
        expectedColors);
  }

  public void doTestFrameRateDifferent(MediaProfileSpecType mediaProfileSpecType,
      String expectedVideoCodec, String expectedAudioCodec, String extension) throws Exception {
    String[] mediaUrls = { getPlayerUrl("/video/10sec/ball25fps.webm"),
        getPlayerUrl("/video/10sec/blue.webm"), getPlayerUrl("/video/10sec/ball25fps.webm") };
    Color[] expectedColors = { Color.BLACK, Color.BLUE, Color.BLACK };

    doTest(mediaProfileSpecType, expectedVideoCodec, expectedAudioCodec, extension, mediaUrls,
        expectedColors);
  }

  public void doTestFrameRateAndFrameSizeDifferent(MediaProfileSpecType mediaProfileSpecType,
      String expectedVideoCodec, String expectedAudioCodec, String extension) throws Exception {
    String[] mediaUrls = { getPlayerUrl("/video/15sec/rgb640x360.mov"),
        getPlayerUrl("/video/15sec/rgb.mov"), getPlayerUrl("/video/15sec/rgb640x360.mov") };
    Color[] expectedColors = { Color.RED, Color.GREEN, Color.RED };

    doTest(mediaProfileSpecType, expectedVideoCodec, expectedAudioCodec, extension, mediaUrls,
        expectedColors);
  }

  public void doTestFrameSizeDifferent(MediaProfileSpecType mediaProfileSpecType,
      String expectedVideoCodec, String expectedAudioCodec, String extension) throws Exception {
    String[] mediaUrls = { getPlayerUrl("/video/format/sintel.webm"),
        getPlayerUrl("/video/format/chrome640x360.mp4"),
        getPlayerUrl("/video/format/sintel.webm") };
    Color[] expectedColors =
      { Color.BLACK, new Color(150, 50, 50), Color.BLACK };

    doTest(mediaProfileSpecType, expectedVideoCodec, expectedAudioCodec, extension, mediaUrls,
        expectedColors);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension, String[] mediaUrls, Color[] expectedColors)
          throws Exception {

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    final CountDownLatch errorPipelinelatch = new CountDownLatch(1);

    mp.addErrorListener(new EventListener<ErrorEvent>() {

      @Override
      public void onEvent(ErrorEvent event) {
        msgError = "Description:" + event.getDescription() + "; Error code:" + event.getType();
        errorPipelinelatch.countDown();
      }
    });

    int numPlayers = mediaUrls.length;
    PlayerEndpoint[] players = new PlayerEndpoint[numPlayers];

    for (int i = 0; i < numPlayers; i++) {
      players[i] = new PlayerEndpoint.Builder(mp, mediaUrls[i]).build();
    }

    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();

    PassThrough passThrough = new PassThrough.Builder(mp).build();

    passThrough.connect(recorderEp);

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    final CountDownLatch recorderLatch = new CountDownLatch(1);

    boolean startRecord = false;
    for (int i = 0; i < numPlayers; i++) {
      players[i].connect(webRtcEp);
      players[i].connect(passThrough);
      players[i].play();

      if (!startRecord) {

        Assert.assertTrue("Not received media (timeout waiting playing event)",
            getPage().waitForEvent("playing"));
        recorderEp.record();
        startRecord = true;
      }

      waitSeconds(PLAYTIME / numPlayers);
    }

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

    Assert.assertTrue(msgError, errorPipelinelatch.getCount() == 1);

    // Reloading browser
    getPage().reload();

    checkRecordingFile(recordingFile, "browser", expectedColors, PLAYTIME, expectedVideoCodec,
        expectedAudioCodec);
    success = true;
  }
}
