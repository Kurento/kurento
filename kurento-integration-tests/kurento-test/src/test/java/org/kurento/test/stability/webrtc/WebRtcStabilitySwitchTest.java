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

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kurento.test.browser.WebRtcChannel.VIDEO_ONLY;
import static org.kurento.test.browser.WebRtcMode.SEND_RCV;
import static org.kurento.test.config.TestScenario.localPresenterAndViewer;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;

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

  private static final int DEFAULT_NUM_SWITCH = 10;
  private static final int PLAYTIME_PER_SWITCH = 15; // seconds

  // test time = PLAYTIME_PER_SWITCH * 2 * DEFAULT_NUM_SWITCH

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return localPresenterAndViewer();
  }

  @Test
  public void testWebRtcStabilitySwitch() throws Exception {
    final int numSwitch = parseInt(getProperty("test.webrtcstability.switch", valueOf(DEFAULT_NUM_SWITCH)));

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint1.connect(webRtcEndpoint1);
    webRtcEndpoint2.connect(webRtcEndpoint2);

    // WebRTC
    getPresenter().subscribeEvents("playing");
    getPresenter().initWebRtc(webRtcEndpoint1, VIDEO_ONLY, SEND_RCV);
    getViewer().subscribeEvents("playing");
    getViewer().initWebRtc(webRtcEndpoint2, VIDEO_ONLY, SEND_RCV);

    for (int i = 0; i < numSwitch; i++) {
      if (i % 2 == 0) {
        log.debug("Switch #" + i + ": loopback");
        webRtcEndpoint1.connect(webRtcEndpoint1);
        webRtcEndpoint2.connect(webRtcEndpoint2);
      } else {
        log.debug("Switch #" + i + ": B2B");
        webRtcEndpoint1.connect(webRtcEndpoint2);
        webRtcEndpoint2.connect(webRtcEndpoint1);
      }
      sleep(SECONDS.toMillis(PLAYTIME_PER_SWITCH));
    }

    // Release Media Pipeline
    mp.release();
  }
}
