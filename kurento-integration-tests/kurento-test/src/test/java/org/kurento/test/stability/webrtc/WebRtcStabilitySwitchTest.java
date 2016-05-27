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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.VideoTagType;

/**
 * Stability test for switching 2 WebRTC (looback to back-2-back) a configurable number of times
 * (each switch holds 1 second).
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (back to back)</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint in loopback to WebRtcEndpoint in B2B.</li>
 * <li>(Browser) 1 WebRtcPeer in send-only sends media. N WebRtcPeer in rcv-only receives media</li>
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
public class WebRtcStabilitySwitchTest extends StabilityTest {

  // test time = PLAYTIME_PER_SWITCH * 2 * DEFAULT_NUM_SWITCH

  // DEFAULT_NUM_SWITCH = 2 --> test time = 1 minute <br/>
  // DEFAULT_NUM_SWITCH = 120 --> test time = 1 hour

  private static final int DEFAULT_NUM_SWITCH = 60;
  private static final int PLAYTIME_PER_SWITCH = 15; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localPresenterAndViewerRgb();
  }

  @Test
  public void testWebRtcStabilitySwitch() throws Exception {
    final int numSwitch = Integer.parseInt(
        System.getProperty("test.webrtcstability.switch", String.valueOf(DEFAULT_NUM_SWITCH)));

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint1.connect(webRtcEndpoint1);
    webRtcEndpoint2.connect(webRtcEndpoint2);

    // WebRTC
    getPresenter().subscribeEvents("playing");
    getPresenter().initWebRtc(webRtcEndpoint1, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);
    getViewer().subscribeEvents("playing");
    getViewer().initWebRtc(webRtcEndpoint2, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);

    // Latency controller
    LatencyController cs1 = new LatencyController("Latency in Browser 1");
    LatencyController cs2 = new LatencyController("Latency in Browser 2");

    try {
      for (int i = 0; i < numSwitch; i++) {

        if (i % 2 == 0) {
          log.debug("Switch #" + i + ": loopback");
          webRtcEndpoint1.connect(webRtcEndpoint1);
          webRtcEndpoint2.connect(webRtcEndpoint2);

          // Latency control (loopback)
          log.debug("[{}.1] Latency control of browser1 to browser1", i);

          cs1.checkLatency(PLAYTIME_PER_SWITCH, TimeUnit.SECONDS, getPresenter());

          log.debug("[{}.2] Latency control of browser2 to browser2", i);
          getViewer().activateLatencyControl(VideoTagType.LOCAL.getId(),
              VideoTagType.REMOTE.getId());
          cs2.checkLatency(PLAYTIME_PER_SWITCH, TimeUnit.SECONDS, getViewer());

        } else {
          log.debug("Switch #" + i + ": B2B");
          webRtcEndpoint1.connect(webRtcEndpoint2);
          webRtcEndpoint2.connect(webRtcEndpoint1);

          // Latency control (B2B)
          log.debug("[{}.3] Latency control of browser1 to browser2", i);
          cs1.checkLatency(PLAYTIME_PER_SWITCH, TimeUnit.SECONDS, getPresenter(), getViewer());

          log.debug("[{}.4] Latency control of browser2 to browser1", i);
          cs2.checkLatency(PLAYTIME_PER_SWITCH, TimeUnit.SECONDS, getViewer(), getPresenter());
        }
      }
    } catch (RuntimeException re) {
      getPresenter().takeScreeshot(getDefaultOutputFile("-browser1-error-screenshot.png"));
      getViewer().takeScreeshot(getDefaultOutputFile("-browser2-error-screenshot.png"));
      Assert.fail(re.getMessage());
    }

    // Draw latency results (PNG chart and CSV file)
    cs1.drawChart(getDefaultOutputFile("-browser1.png"), 500, 270);
    cs1.writeCsv(getDefaultOutputFile("-browser1.csv"));
    cs1.logLatencyErrorrs();

    cs2.drawChart(getDefaultOutputFile("-browser2.png"), 500, 270);
    cs2.writeCsv(getDefaultOutputFile("-browser2.csv"));
    cs2.logLatencyErrorrs();

    // Release Media Pipeline
    mp.release();
  }
}
