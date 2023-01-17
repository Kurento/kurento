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

package org.kurento.test.stability.webrtc;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.VideoTagType;

/**
 * Stability test for WebRTC in loopback during a long time (configurable).
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint in loopback.</li>
 * <li>(Browser) WebRtcPeer in send-receive mode sends and receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color change should be detected on local/remote video tag of browsers</li>
 * <li>Test fail when 3 consecutive latency errors (latency > 3sec) are detected</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcStabilityLoopbackTest extends StabilityTest {

  private static final int DEFAULT_PLAYTIME = 30; // minutes

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    String videoPath = KurentoTest.getTestFilesDiskPath() + "/video/15sec/rgbHD.y4m";
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.BROWSER, new Browser.Builder().webPageType(WebPageType.WEBRTC)
        .browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).video(videoPath).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testWebRtcStabilityLoopback() throws Exception {
    final int playTime = Integer.parseInt(
        System.getProperty("test.webrtcstability.playtime", String.valueOf(DEFAULT_PLAYTIME)));

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    // WebRTC
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);

    // Latency assessment
    LatencyController cs = new LatencyController("WebRTC in loopback");
    getPage().activateLatencyControl(VideoTagType.LOCAL.getId(), VideoTagType.REMOTE.getId());
    cs.checkLatency(playTime, TimeUnit.MINUTES, getPage());

    // Release Media Pipeline
    mp.release();

    // Draw latency results (PNG chart and CSV file)
    cs.drawChart(getDefaultOutputFile(".png"), 500, 270);
    cs.writeCsv(getDefaultOutputFile(".csv"));
    cs.logLatencyErrorrs();
  }
}
