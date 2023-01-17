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
import java.util.Arrays;
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
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Test of a Recorder switching sources from WebRtc Endpoint
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> RecorderEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>2 x Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Two media pipelines. First WebRtcEndpoint to RecorderEndpoint (recording) and then
 * PlayerEndpoint -> WebRtcEndpoint (play of the recording).</li>
 * <li>(Browser) First a WebRtcPeer in send-only sends media. Second, other WebRtcPeer in rcv-only
 * receives media</li>
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
public class RecorderWebRtcDisconnectTest extends BaseRecorder {

  private static final int PLAYTIME = 10; // seconds
  private static final int NUM_SWAPS = 6;
  private static final Color[] EXPECTED_COLORS = { Color.RED };

  private static final String BROWSER1 = "browser1";
  private static final String BROWSER2 = "browser2";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    TestScenario test = new TestScenario();
    test.addBrowser(BROWSER1,
        new Browser.Builder().browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
        .webPageType(WebPageType.WEBRTC).video(getTestFilesDiskPath() + "/video/10sec/red.y4m")
        .build());
    test.addBrowser(BROWSER2, new Browser.Builder().browserType(BrowserType.CHROME)
        .scope(BrowserScope.LOCAL).webPageType(WebPageType.WEBRTC).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testRecorderWebRtcDisconnectWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderWebRtcDisconnectMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
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

    // Test execution
    getPage(BROWSER1).subscribeLocalEvents("playing");
    getPage(BROWSER1).initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

    // red
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER1).waitForEvent("playing"));
    recorderEp.record();
    for (int i = 0; i < NUM_SWAPS; i++) {
      if (i % 2 == 0) {
        webRtcEp.connect(recorderEp);
      } else {
        webRtcEp.disconnect(recorderEp);
      }

      Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / NUM_SWAPS);
    }

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
    getPage(BROWSER1).close();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp2 = new PlayerEndpoint.Builder(mp2, recordingFile).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEp2.connect(webRtcEp2);

    // Playing the recording
    getPage(BROWSER2).subscribeEvents("playing");
    getPage(BROWSER2).initWebRtc(webRtcEp2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
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
        getPage(BROWSER2).waitForEvent("playing"));
    for (Color color : EXPECTED_COLORS) {
      Assert.assertTrue("The color of the recorded video should be " + color + " " + messageAppend,
          getPage(BROWSER2).similarColor(color));
    }
    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(getPage(BROWSER2).getTimeout(), TimeUnit.SECONDS));

    double currentTime = getPage(BROWSER2).getCurrentTime();
    Assert.assertTrue(
        "Error in play time in the recorded video (expected: " + playtime + " sec, real: "
            + currentTime + " sec) " + messageAppend,
            getPage(BROWSER2).compare(playtime, currentTime));

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, TimeUnit.SECONDS.toMillis(playtime),
        TimeUnit.SECONDS.toMillis(getPage(BROWSER2).getThresholdTime()));

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }

}
