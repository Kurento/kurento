/*
 * (C) Copyright 2018 Kurento (http://kurento.org/)
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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;

/**
 * WebRTC one to one test.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint presenter connected to 1 viewer</li>
 * <li>(Browser) 1 WebRtcPeer in send-only sends media. 1 WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag of the viewers</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.7.2
 */
public class WebRtcOneToOneTest extends FunctionalTest {

  private static final int PLAYTIME = 30; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    // Test: 1 presenter and 1 viewer
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.PRESENTER,
        new Browser.Builder().webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
            .scope(BrowserScope.LOCAL).build());
    test.addBrowser(BrowserConfig.VIEWER,
        new Browser.Builder().webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
            .scope(BrowserScope.LOCAL).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testWebRtcOneToOneChrome() throws Exception {
    // Media Pipeline
    final MediaPipeline mp = kurentoClient.createMediaPipeline();
    final WebRtcEndpoint masterWebRtcEp = new WebRtcEndpoint.Builder(mp).build();
    final WebRtcEndpoint viewerWebRtcEP = new WebRtcEndpoint.Builder(mp).build();
    masterWebRtcEp.connect(viewerWebRtcEP);

    // WebRTC setup
    getPresenter().initWebRtc(masterWebRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getViewer().initWebRtc(viewerWebRtcEP, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
    getViewer().subscribeEvents("playing");
    getViewer().waitForEvent("playing");

    // Guard time to play the video
    waitSeconds(PLAYTIME);

    // Assertions
    double currentTime = getViewer().getCurrentTime();
    Assert.assertTrue("Error in play time (expected: " + PLAYTIME + " sec, real: "
        + currentTime + " sec)", getViewer().compare(PLAYTIME, currentTime));

    // Release Media Pipeline
    mp.release();
  }
}
