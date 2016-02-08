/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.test.functional.webrtc;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * WebRTC in loopback.
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
 * <li>(Browser) WebRtcPeer in send-receive mode sends and receives media</li>
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
 * @since 4.2.3
 */

public class WebRtcOneLoopbackTest extends FunctionalTest {

  private static final int PLAYTIME = 10; // seconds to play in WebRTC

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testWebRtcLoopback() throws InterruptedException {

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    // Start WebRTC and wait for playing event
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    // Guard time to play the video
    waitSeconds(PLAYTIME);

    // Assertions
    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue(
        "Error in play time (expected: " + PLAYTIME + " sec, real: " + currentTime + " sec)",
        getPage().compare(PLAYTIME, currentTime));
    Assert.assertTrue("The color of the video should be green",
        getPage().similarColor(CHROME_VIDEOTEST_COLOR));

    // Release Media Pipeline
    mp.release();
  }
}
