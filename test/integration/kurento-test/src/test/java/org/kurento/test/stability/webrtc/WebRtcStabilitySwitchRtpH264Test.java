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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
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
import org.kurento.test.sdp.SdpUtils;

/**
 * Stability test for switching a WebRTC connected to RTP performing H264 transcoding.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> RtpEndpoint1</li>
 * <li>RtpEndpoint1 -> RtpEndpoint2 (RTP session)</li>
 * <li>RtpEndpoint2 -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint to RtpEndpoint. RtpEndpoint to RtpEndpoint. RtpEndpoint to
 * WebRtcEndpoint.</li>
 * <li>(Browser) WebRtcPeer in send-receive mode sends and receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>Color change should be detected on local and remote video tags</li>
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
public class WebRtcStabilitySwitchRtpH264Test extends StabilityTest {

  private static final int DEFAULT_PLAYTIME = 30; // minutes
  private static final String[] REMOVE_CODECS = { "H263-1998", "VP8", "MP4V-ES" };

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    String videoPath = KurentoTest.getTestFilesDiskPath() + "/video/15sec/rgbHD.y4m";
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.BROWSER, new Browser.Builder().webPageType(WebPageType.WEBRTC)
        .browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).video(videoPath).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testWebRtcStabilitySwitchRtpH264() throws Exception {
    final int playTime =
        Integer.parseInt(System.getProperty("test.webrtc.stability.switch.webrtc2rtp.playtime",
            String.valueOf(DEFAULT_PLAYTIME)));

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    RtpEndpoint rtpEndpoint1 = new RtpEndpoint.Builder(mp).build();
    RtpEndpoint rtpEndpoint2 = new RtpEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(rtpEndpoint1);
    rtpEndpoint2.connect(webRtcEndpoint);

    // RTP session (rtpEndpoint1 --> rtpEndpoint2)
    String sdpOffer = rtpEndpoint1.generateOffer();
    log.debug("SDP offer in rtpEndpoint1\n{}", sdpOffer);

    // SDP mangling
    sdpOffer = SdpUtils.mangleSdp(sdpOffer, REMOVE_CODECS);
    log.debug("SDP offer in rtpEndpoint1 after mangling\n{}", sdpOffer);

    String sdpAnswer1 = rtpEndpoint2.processOffer(sdpOffer);
    log.debug("SDP answer in rtpEndpoint2\n{}", sdpAnswer1);
    String sdpAnswer2 = rtpEndpoint1.processAnswer(sdpAnswer1);
    log.debug("SDP answer in rtpEndpoint1\n{}", sdpAnswer2);

    // Latency controller
    LatencyController cs = new LatencyController();

    // WebRTC
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);

    // Assertion: wait to playing event in browser
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    // Latency assessment
    getPage().activateLatencyControl(VideoTagType.LOCAL.getId(), VideoTagType.REMOTE.getId());
    cs.checkLatencyInBackground(playTime, TimeUnit.MINUTES, getPage());

    // Connect-disconnect each second
    for (int i = 0; i < DEFAULT_PLAYTIME * 60; i++) {
      Thread.sleep(TimeUnit.SECONDS.toMillis(1));
      rtpEndpoint2.disconnect(webRtcEndpoint);
      rtpEndpoint2.connect(webRtcEndpoint);
    }

    // Release Media Pipeline
    mp.release();

    // Draw latency results (PNG chart and CSV file)
    cs.drawChart(getDefaultOutputFile(".png"), 500, 270);
    cs.writeCsv(getDefaultOutputFile(".csv"));
    cs.logLatencyErrorrs();
  }
}
