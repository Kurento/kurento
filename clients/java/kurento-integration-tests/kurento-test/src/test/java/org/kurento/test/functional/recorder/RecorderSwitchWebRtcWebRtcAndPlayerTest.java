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
import org.kurento.client.ErrorEvent;
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

/**
 * Test of a Recorder switching sources from WebRtc Endpoint and Player Endpoint
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> RecorderEndpoint; WebRtcEndpoint -> RecorderEndpoint</li>
 * <li>WebRtcEndpoint -> RecorderEndpoint; PlayerEndpoint -> RecorderEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li></li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Two media pipelines. First WebRtcEndpoint to RecorderEndpoint (recording) and then
 * PlayerEndpoint -> RecorderEndpoint (recording). And the second PlayerEndpoint -> WebRtcEndpoint
 * (play of the recording).</li>
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
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */
public class RecorderSwitchWebRtcWebRtcAndPlayerTest extends BaseRecorder {

  private static final int PLAYTIME = 30; // seconds
  private static final int N_PLAYER = 3;
  private String msgError = "";
  private static final Color[] EXPECTED_COLORS = { Color.RED, Color.GREEN, Color.RED };

  private static final String BROWSER1 = "browser1";
  private static final String BROWSER2 = "browser2";
  private static final String BROWSER3 = "browser3";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    TestScenario test = new TestScenario();
    test.addBrowser(BROWSER1,
        new Browser.Builder().browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
            .webPageType(WebPageType.WEBRTC).video(getTestFilesDiskPath() + "/video/10sec/red.y4m")
            .build());
    test.addBrowser(BROWSER2,
        new Browser.Builder().browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
            .webPageType(WebPageType.WEBRTC)
            .video(getTestFilesDiskPath() + "/video/10sec/green.y4m").build());
    test.addBrowser(BROWSER3, new Browser.Builder().browserType(BrowserType.CHROME)
        .scope(BrowserScope.LOCAL).webPageType(WebPageType.WEBRTC).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testRecorderSwitchWebRtcWebRtcWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  @Test
  public void testRecorderSwitchWebRtcWebRtcMp4() throws Exception {
    doTest(MP4, EXPECTED_VIDEO_CODEC_MP4, EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
  }

  @Test
  public void testRecorderSwitchWebRtcPlayerWebm() throws Exception {
    doTestWithPlayer(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM,
        getPlayerUrl("/video/15sec/rgb640x360.webm"));
  }

  @Test
  public void testRecorderSwitchWebRtcPlayerMp4() throws Exception {
    doTestWithPlayer(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM,
        getPlayerUrl("/video/15sec/rgb.mp4"));
  }

  public void doTestWithPlayer(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension, String mediaUrlPlayer) throws Exception {
    // Media Pipeline #1
    getPage(BROWSER2).close();
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    final CountDownLatch errorPipelinelatch = new CountDownLatch(1);

    mp.addErrorListener(new EventListener<ErrorEvent>() {

      @Override
      public void onEvent(ErrorEvent event) {
        msgError = "Description:" + event.getDescription() + "; Error code:" + event.getType();
        errorPipelinelatch.countDown();
      }
    });

    WebRtcEndpoint webRtcEpRed = new WebRtcEndpoint.Builder(mp).build();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrlPlayer).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();

    // Test execution
    getPage(BROWSER1).subscribeLocalEvents("playing");
    long startWebrtc = System.currentTimeMillis();
    getPage(BROWSER1).initWebRtc(webRtcEpRed, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

    webRtcEpRed.connect(recorderEp);
    recorderEp.record();

    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER1).waitForEvent("playing"));
    long webrtcRedConnectionTime = System.currentTimeMillis() - startWebrtc;
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    startWebrtc = System.currentTimeMillis();

    playerEp.play();
    playerEp.connect(recorderEp);
    long playerEpConnectionTime = System.currentTimeMillis() - startWebrtc;
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    webRtcEpRed.connect(recorderEp);
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

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
        recorderLatch.await(getPage(BROWSER1).getTimeout(), TimeUnit.SECONDS));
    mp.release();

    Assert.assertTrue(msgError, errorPipelinelatch.getCount() == 1);

    final long playtime = PLAYTIME
        + TimeUnit.MILLISECONDS.toSeconds((2 * webrtcRedConnectionTime) + playerEpConnectionTime);

    checkRecordingFile(recordingFile, BROWSER3, EXPECTED_COLORS, playtime, expectedVideoCodec,
        expectedAudioCodec);
    success = true;
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {
    // Media Pipeline #1
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    final CountDownLatch errorPipelinelatch = new CountDownLatch(1);

    mp.addErrorListener(new EventListener<ErrorEvent>() {

      @Override
      public void onEvent(ErrorEvent event) {
        msgError = "Description:" + event.getDescription() + "; Error code:" + event.getType();
        errorPipelinelatch.countDown();
      }
    });

    WebRtcEndpoint webRtcEpRed = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEpGreen = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile = getRecordUrl(extension);
    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();

    // Test execution
    getPage(BROWSER1).subscribeLocalEvents("playing");
    long startWebrtc = System.currentTimeMillis();
    getPage(BROWSER1).initWebRtc(webRtcEpRed, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

    webRtcEpRed.connect(recorderEp);
    recorderEp.record();

    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER1).waitForEvent("playing"));
    long webrtcRedConnectionTime = System.currentTimeMillis() - startWebrtc;
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    getPage(BROWSER2).subscribeLocalEvents("playing");
    startWebrtc = System.currentTimeMillis();
    getPage(BROWSER2).initWebRtc(webRtcEpGreen, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);

    // green
    webRtcEpGreen.connect(recorderEp);

    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER2).waitForEvent("playing"));
    long webrtcGreenConnectionTime = System.currentTimeMillis() - startWebrtc;
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    webRtcEpRed.connect(recorderEp);

    startWebrtc = System.currentTimeMillis();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

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
        recorderLatch.await(getPage(BROWSER2).getTimeout(), TimeUnit.SECONDS));
    mp.release();

    Assert.assertTrue(msgError, errorPipelinelatch.getCount() == 1);

    final long playtime = PLAYTIME + TimeUnit.MILLISECONDS
        .toSeconds((2 * webrtcRedConnectionTime) + webrtcGreenConnectionTime);

    checkRecordingFile(recordingFile, BROWSER3, EXPECTED_COLORS, playtime, expectedVideoCodec,
        expectedAudioCodec);
    success = true;
  }

}
