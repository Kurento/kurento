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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EventListener;
import org.kurento.client.MediaFlowInStateChangedEvent;
import org.kurento.client.MediaFlowState;
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
    return TestScenario.localChrome();
  }

  @Test
  public void testWebRtcLoopback() throws Exception {

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    final CountDownLatch flowingLatch = new CountDownLatch(1);
    webRtcEndpoint
        .addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {

          @Override
          public void onEvent(MediaFlowInStateChangedEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              flowingLatch.countDown();
            }
          }
        });

    // Start WebRTC and wait for playing event
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

    Assert.assertTrue("Not received FLOWING IN event in webRtcEp: " + WebRtcChannel.AUDIO_AND_VIDEO,
        flowingLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    Assert.assertTrue(
        "Not received media (timeout waiting playing event): " + WebRtcChannel.AUDIO_AND_VIDEO,
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
