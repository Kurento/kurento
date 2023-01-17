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
import java.util.Arrays;
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
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.functional.recorder.BaseRecorder;

/**
 * Four synthetic videos are played by four WebRtcEndpoint (Connect only audio for thres videos) and
 * mixed by a Composite. The resulting video is recording using a RecorderEndpoint. The recorded
 * video is played using a PlayerEndpoint.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>4xWebRtcEndpoint -> Composite -> RecorderEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Media server mix the media from 4 WebRtcEndpoint and it records the grid using the
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

public class CompositeWebRtcRecorderTest extends BaseRecorder {

  private static int PLAYTIME = 10; // seconds

  private static final String BROWSER1 = "browser1";
  private static final String BROWSER2 = "browser2";
  private static final String BROWSER3 = "browser3";
  private static final String BROWSER4 = "browser4";
  private static final String BROWSER5 = "browser5";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    // Test: 5 local Chrome's
    TestScenario test = new TestScenario();
    test.addBrowser(BROWSER1, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    test.addBrowser(BROWSER2,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/red.y4m")
            .build());
    test.addBrowser(BROWSER3,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/green.y4m")
            .build());
    test.addBrowser(BROWSER4,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/blue.y4m")
            .build());
    test.addBrowser(BROWSER5,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/white.y4m")
            .build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testCompositeRecorder() throws Exception {

    // MediaPipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    Composite composite = new Composite.Builder(mp).build();

    HubPort hubPort1 = new HubPort.Builder(composite).build();
    WebRtcEndpoint webRtcEpRed = new WebRtcEndpoint.Builder(mp).build();
    webRtcEpRed.connect(hubPort1);

    HubPort hubPort2 = new HubPort.Builder(composite).build();
    WebRtcEndpoint webRtcEpGreen = new WebRtcEndpoint.Builder(mp).build();
    webRtcEpGreen.connect(hubPort2, MediaType.AUDIO);

    HubPort hubPort3 = new HubPort.Builder(composite).build();
    WebRtcEndpoint webRtcEpBlue = new WebRtcEndpoint.Builder(mp).build();
    webRtcEpBlue.connect(hubPort3, MediaType.AUDIO);

    HubPort hubPort4 = new HubPort.Builder(composite).build();
    WebRtcEndpoint webRtcEpWhite = new WebRtcEndpoint.Builder(mp).build();
    webRtcEpWhite.connect(hubPort4, MediaType.AUDIO);

    String recordingFile = getDefaultOutputFile(EXTENSION_WEBM);
    RecorderEndpoint recorderEp =
        new RecorderEndpoint.Builder(mp, Protocol.FILE + recordingFile).build();
    HubPort hubPort5 = new HubPort.Builder(composite).build();
    hubPort5.connect(recorderEp);

    // WebRTC browsers
    getPage(BROWSER2).initWebRtc(webRtcEpRed, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(BROWSER3).initWebRtc(webRtcEpGreen, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);
    getPage(BROWSER4).initWebRtc(webRtcEpBlue, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(BROWSER5).initWebRtc(webRtcEpWhite, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);

    recorderEp.record();

    Thread.sleep(PLAYTIME * 1000);

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

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp2 =
        new PlayerEndpoint.Builder(mp2, Protocol.FILE + recordingFile).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEp2.connect(webRtcEp2);

    // Playing the recorded file
    launchBrowser(mp2, webRtcEp2, playerEp2, null, EXPECTED_VIDEO_CODEC_WEBM,
        EXPECTED_AUDIO_CODEC_WEBM, recordingFile, Color.RED, 0, 0, PLAYTIME);

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }
}
