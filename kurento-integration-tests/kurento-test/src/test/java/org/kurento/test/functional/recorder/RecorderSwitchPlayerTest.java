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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
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

/**
 * Test of a Recorder switching sources from PlayerEndpoint. </p> Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> RecorderEndpoint & WebRtcEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
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
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderSwitchPlayerTest extends BaseRecorder {

  private static final int PLAYTIME = 20; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testRecorderSwitchSameFormatPlayerWebm() throws Exception {
    doTestSameFormats(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchSameFormatPlayerMp4() throws Exception {
    doTestSameFormats(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  @Test
  public void testRecorderSwitchDifferentFormatPlayerWebm() throws Exception {
    doTestDifferentFormats(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM,
        EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchDifferentFormatPlayerMp4() throws Exception {
    doTestDifferentFormats(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  public void doTestSameFormats(MediaProfileSpecType mediaProfileSpecType,
      String expectedVideoCodec, String expectedAudioCodec, String extension) throws Exception {
    String[] mediaUrls =
      { getPlayerUrl("/video/10sec/red.webm"), getPlayerUrl("/video/10sec/green.webm"),
        getPlayerUrl("/video/10sec/blue.webm") };
    Color[] expectedColors = { Color.RED, Color.GREEN, Color.BLUE };

    doTest(mediaProfileSpecType, expectedVideoCodec, expectedAudioCodec, extension, mediaUrls,
        expectedColors);
  }

  public void doTestDifferentFormats(MediaProfileSpecType mediaProfileSpecType,
      String expectedVideoCodec, String expectedAudioCodec, String extension) throws Exception {
    String[] mediaUrls =
      { getPlayerUrl("/video/10sec/ball.mkv"), getPlayerUrl("/video/10sec/white.webm"),
        getPlayerUrl("/video/10sec/blue.m4v") };
    Color[] expectedColors = { Color.BLACK, Color.WHITE, Color.BLUE };

    doTest(mediaProfileSpecType, expectedVideoCodec, expectedAudioCodec, extension, mediaUrls,
        expectedColors);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension, String[] mediaUrls, Color[] expectedColors)
          throws Exception {

    // Media Pipeline #1
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    int numPlayers = mediaUrls.length;
    PlayerEndpoint[] players = new PlayerEndpoint[numPlayers];

    for (int i = 0; i < numPlayers; i++) {
      players[i] = new PlayerEndpoint.Builder(mp, mediaUrls[i]).build();
    }

    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp =
        new RecorderEndpoint.Builder(mp, recordingFile).withMediaProfile(mediaProfileSpecType)
        .build();

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    boolean startRecord = false;
    for (int i = 0; i < numPlayers; i++) {
      players[i].connect(webRtcEp);
      players[i].connect(recorderEp);
      players[i].play();

      if (!startRecord) {

        Assert.assertTrue("Not received media (timeout waiting playing event)", getPage()
            .waitForEvent("playing"));
        recorderEp.record();
        startRecord = true;
      }

      waitSeconds(PLAYTIME / numPlayers);
    }

    // Release Media Pipeline #1
    saveGstreamerDot(mp);
    recorderEp.stop();
    mp.release();

    // Reloading browser
    getPage().reload();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp2 = new PlayerEndpoint.Builder(mp2, recordingFile).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEp2.connect(webRtcEp2);

    // Playing the recording
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });
    playerEp2.play();

    // Assertions in recording
    Assert.assertTrue("Not received media in the recording (timeout waiting playing event)",
        getPage().waitForEvent("playing"));
    for (Color color : expectedColors) {
      Assert.assertTrue("The color of the recorded video should be " + color, getPage()
          .similarColor(color));
    }
    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue("Error in play time in the recorded video (expected: " + PLAYTIME
        + " sec, real: " + currentTime + " sec) ", getPage().compare(PLAYTIME, currentTime));

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, TimeUnit.SECONDS.toMillis(PLAYTIME),
        TimeUnit.SECONDS.toMillis(getPage().getThresholdTime()));

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }

}
