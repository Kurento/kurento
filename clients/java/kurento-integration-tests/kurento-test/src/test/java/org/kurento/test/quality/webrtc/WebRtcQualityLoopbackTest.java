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

package org.kurento.test.quality.webrtc;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.base.QualityTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.AudioChannel;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.utils.Ffmpeg;

/**
 * WebRTC in loopback using custom video and audio files.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint in loopback <br>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Perceived audio quality should be fair (PESQMOS)</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>Play time in remote video should be as expected</li>
 * <li>The color of the received video should be as expected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class WebRtcQualityLoopbackTest extends QualityTest {

  private static int PLAYTIME = 10; // seconds to play in WebRTC
  private static int AUDIO_SAMPLE_RATE = 16000; // samples per second
  private static float MIN_PESQ_MOS = 3; // Audio quality (PESQ MOS [1..5])

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    String videoPath = KurentoTest.getTestFilesDiskPath() + "/video/10sec/red.y4m";
    String audioUrl = "http://" + getTestFilesHttpPath() + "/audio/10sec/fiware_mono_16khz.wav";
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.BROWSER,
        new Browser.Builder().webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
            .scope(BrowserScope.LOCAL).video(videoPath)
            .audio(audioUrl, PLAYTIME, AUDIO_SAMPLE_RATE, AudioChannel.MONO).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Ignore
  @Test
  public void testWebRtcQualityLoopback() throws InterruptedException {
    doTest(BrowserType.CHROME, getTestFilesDiskPath() + "/video/10sec/red.y4m",
        "http://" + getTestFilesHttpPath() + "/audio/10sec/fiware_mono_16khz.wav", Color.RED);
  }

  public void doTest(BrowserType browserType, String videoPath, String audioUrl, Color color)
      throws InterruptedException {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

    // Wait until event playing in the remote stream
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    // Guard time to play the video
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

    // Assert play time
    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue("Error in play time of player (expected: " + PLAYTIME + " sec, real: "
        + currentTime + " sec)", getPage().compare(PLAYTIME, currentTime));

    // Assert color
    if (color != null) {
      Assert.assertTrue("The color of the video should be " + color, getPage().similarColor(color));
    }

    // Assert audio quality
    if (audioUrl != null) {
      float realPesqMos = Ffmpeg.getPesqMos(audioUrl, AUDIO_SAMPLE_RATE);
      Assert.assertTrue("Bad perceived audio quality: PESQ MOS too low (expected=" + MIN_PESQ_MOS
          + ", real=" + realPesqMos + ")", realPesqMos >= MIN_PESQ_MOS);
    }

    // Release Media Pipeline
    mp.release();
  }
}
