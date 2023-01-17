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

package org.kurento.test.stability.webrtc;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;

/**
 * Stability test for switching a WebRTC in one to one communication.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (back-to-back)(x2)</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>2 x Chrome (presenter and viewer)</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint presenter and viewer</li>
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
 * @since 5.1.0
 */
public class WebRtcStabilityBack2BackTest extends StabilityTest {

  private static final int DEFAULT_PLAYTIME = 30; // minutes

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localPresenterAndViewerRgb();
  }

  @Test
  public void testWebRtcStabilityBack2Back() throws Exception {
    final int playTime = Integer.parseInt(System
        .getProperty("test.webrtc.stability.back2back.playtime", String.valueOf(DEFAULT_PLAYTIME)));

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint1.connect(webRtcEndpoint2);
    webRtcEndpoint2.connect(webRtcEndpoint1);

    // Latency control
    LatencyController cs = new LatencyController("WebRTC latency control");

    // WebRTC
    getPresenter().subscribeLocalEvents("playing");
    getPresenter().initWebRtc(webRtcEndpoint1, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_ONLY);
    getViewer().subscribeEvents("playing");
    getViewer().initWebRtc(webRtcEndpoint2, WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);

    // Latency assessment
    cs.checkLatency(playTime, TimeUnit.MINUTES, getPresenter(), getViewer());

    // Release Media Pipeline
    mp.release();

    // Draw latency results (PNG chart and CSV file)
    cs.drawChart(getDefaultOutputFile(".png"), 500, 270);
    cs.writeCsv(getDefaultOutputFile(".csv"));
    cs.logLatencyErrorrs();
  }
}
