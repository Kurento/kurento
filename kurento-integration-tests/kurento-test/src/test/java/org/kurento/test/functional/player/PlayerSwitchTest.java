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

package org.kurento.test.functional.player;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Test of N PlayerEndpoints connected to the same WebRtcEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) N PlayerEndpoints switch media to a WebRtcEndpoint<br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · Play time in remote video should be as expected <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class PlayerSwitchTest extends FunctionalTest {

  private static final int PLAYTIME = 30; // seconds
  private static final int N_PLAYER = 5;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testPlayerSwitch() throws Exception {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerRed =
        new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/format/chrome.mp4").build();
    PlayerEndpoint playerGreen =
        new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/format/fiware.mkv").build();
    PlayerEndpoint playerBlue =
        new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/format/sintel.webm").build();
    PlayerEndpoint playerBall =
        new PlayerEndpoint.Builder(mp, "http://files.kurento.org/video/format/rabbit.mov").build();
    PlayerEndpoint playerRtsp =
        new PlayerEndpoint.Builder(mp, "rtsp://195.55.223.100/axis-media/media.amp").build();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    // red
    playerRed.connect(webRtcEndpoint);
    playerRed.play();
    getPage().subscribeEvents("playing");
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // green
    playerGreen.connect(webRtcEndpoint);
    playerGreen.play();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // blue
    playerBlue.connect(webRtcEndpoint);
    playerBlue.play();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // ball
    playerBall.connect(webRtcEndpoint);
    playerBall.play();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // rtsp
    playerRtsp.connect(webRtcEndpoint);
    playerRtsp.play();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));
    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue(
        "Error in play time (expected: " + PLAYTIME + " sec, real: " + currentTime + " sec)",
        getPage().compare(PLAYTIME, currentTime));

    // Release Media Pipeline
    mp.release();
  }

}
