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
import org.kurento.test.browser.ConsoleLogLevel;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Back-To-Back WebRTC switch. Three clients: A,B,C sets up WebRTC send-recv with audio/video.
 * Switch between following scenarios: A<->B, A<->C, B<->C. <br>
 *
 * Media Pipeline(s): <br>
 * · WebRtcEndpoint -> WebRtcEndpoint <br>
 * · WebRtcEndpoint -> WebRtcEndpoint <br>
 * · WebRtcEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) WebRtcEndpoint in loopback <br>
 * 2. (Browser) WebRtcPeer in send-receive mode and receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · Play time in remote video should be as expected <br>
 * · The color of the received video should be as expected <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class WebRtcSwitchTest extends FunctionalTest {

  private static final int PLAYTIME = 5; // seconds
  private static final int NUM_BROWSERS = 3;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromesAndFirefoxs(NUM_BROWSERS);
  }

  @Test
  public void testWebRtcSwitch() throws InterruptedException {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoints[] = new WebRtcEndpoint[NUM_BROWSERS];

    for (int i = 0; i < NUM_BROWSERS; i++) {
      webRtcEndpoints[i] = new WebRtcEndpoint.Builder(mp).build();
      webRtcEndpoints[i].connect(webRtcEndpoints[i]);

      // Start WebRTC in loopback in each browser
      getPage(i).subscribeEvents("playing");
      getPage(i).initWebRtc(webRtcEndpoints[i], WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

      // Delay time (to avoid the same timing in videos)
      waitSeconds(1);

      // Wait until event playing in the remote streams
      Assert.assertTrue("Not received media #1 (timeout waiting playing event)", getPage(i)
          .waitForEvent("playing"));

      // Assert color
      assertColor(i);
    }

    // Guard time to see switching #0
    waitSeconds(PLAYTIME);

    // Switching (round #1)
    for (int i = 0; i < NUM_BROWSERS; i++) {
      int next = (i + 1) >= NUM_BROWSERS ? 0 : i + 1;
      webRtcEndpoints[i].connect(webRtcEndpoints[next]);
      getPage(i).consoleLog(ConsoleLogLevel.INFO,
          "Switch #1: webRtcEndpoint" + i + " -> webRtcEndpoint" + next);
      // Assert color
      assertColor(i);
    }

    // Guard time to see switching #1
    waitSeconds(PLAYTIME);

    // Switching (round #2)
    for (int i = 0; i < NUM_BROWSERS; i++) {
      int previous = (i - 1) < 0 ? NUM_BROWSERS - 1 : i - 1;
      webRtcEndpoints[i].connect(webRtcEndpoints[previous]);
      getPage(i).consoleLog(ConsoleLogLevel.INFO,
          "Switch #2: webRtcEndpoint" + i + " -> webRtcEndpoint" + previous);
      // Assert color
      assertColor(i);
    }

    // Guard time to see switching #2
    waitSeconds(PLAYTIME);

    // Release Media Pipeline
    mp.release();
  }

  public void assertColor(int index) {
    Assert.assertTrue("The color of the video should be green",
        getPage(index).similarColor(CHROME_VIDEOTEST_COLOR));
  }

}
