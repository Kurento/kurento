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

package org.kurento.test.functional.webrtc;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * WebRTC in loopback with a FaceOverlayFilter.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> FaceOverlayFilter -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint in loopback with a FaceOverlayFilter</li>
 * <li>(Browser) WebRtcPeer in send-receive mode and receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>Play time in remote video should be as expected</li>
 * <li>The color of the received video should be as expected</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */

public class WebRtcOneFaceOverlayTest extends FunctionalTest {

  private static final int DEFAULT_PLAYTIME = 10; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testWebRtcFaceOverlay() throws InterruptedException {

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(mp).build();
    webRtcEndpoint.connect(faceOverlayFilter);
    faceOverlayFilter.connect(webRtcEndpoint);

    // Start WebRTC and wait for playing event
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    // Guard time to play the video
    int playTime = Integer.parseInt(
        System.getProperty("test.webrtcfaceoverlay.playtime", String.valueOf(DEFAULT_PLAYTIME)));
    waitSeconds(playTime);

    // Assertions
    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue(
        "Error in play time (expected: " + playTime + " sec, real: " + currentTime + " sec)",
        getPage().compare(playTime, currentTime));
    Assert.assertTrue("The color of the video should be green",
        getPage().similarColor(CHROME_VIDEOTEST_COLOR));

    // Release Media Pipeline
    mp.release();
  }
}
