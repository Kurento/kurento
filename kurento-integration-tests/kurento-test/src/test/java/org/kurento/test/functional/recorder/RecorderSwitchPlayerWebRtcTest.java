/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the
 * terms of the GNU Lesser General Public License (LGPL) version 2.1 which
 * accompanies this
 * distribution, and is available at http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU
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
 * Test of a Recorder switching sources from WebRtc Endpoint </p> Media Pipeline(s):
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
    Assert.assertTrue("Not received media (timeout waiting playing event)", getPage(0)
        .waitForEvent("playing"));

    PlayerEndpoint playerGreen =
        new PlayerEndpoint.Builder(mp, getPlayerUrl("/video/10sec/green.webm")).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp =
        new RecorderEndpoint.Builder(mp, recordingFile).withMediaProfile(mediaProfileSpecType)
        .build();

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
    recorderEp.stop();
    mp.release();

    // Wait until file exists
    waitForFileExists(recordingFile);

    // Reloading browser
    getPage(0).close();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp2 = new PlayerEndpoint.Builder(mp2, recordingFile).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEp2.connect(webRtcEp2);

    // Playing the recording
    getPage(1).subscribeEvents("playing");
    getPage(1).initWebRtc(webRtcEp2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
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

    Assert.assertTrue("Not received media in the recording (timeout waiting playing event) "
        + messageAppend, getPage(1).waitForEvent("playing"));
    for (Color color : EXPECTED_COLORS) {
      Assert.assertTrue("The color of the recorded video should be " + color + " " + messageAppend,
          getPage(1).similarColor(color));
    }
    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(getPage(1).getTimeout(), TimeUnit.SECONDS));

    double currentTime = getPage(1).getCurrentTime();
    Assert.assertTrue("Error in play time in the recorded video (expected: " + playtime
        + " sec, real: " + currentTime + " sec) " + messageAppend,
        getPage(1).compare(playtime, currentTime));

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, TimeUnit.SECONDS.toMillis(playtime),
        TimeUnit.SECONDS.toMillis(getPage(1).getThresholdTime()));

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }
}
