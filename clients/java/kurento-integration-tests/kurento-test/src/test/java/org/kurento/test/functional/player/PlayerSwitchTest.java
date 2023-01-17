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
 * Test of N PlayerEndpoints connected to the same WebRtc Endpoint
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) N PlayerEndpoints switch media to a WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>Play time in remote video should be as expected</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
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
    PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/format/chrome.mp4").build();

    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    // red
    playerRed.connect(webRtcEndpoint);
    playerRed.play();
    getPage().subscribeEvents("playing");
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/format/fiware.mkv").build();
    // green
    playerGreen.connect(webRtcEndpoint);
    playerGreen.play();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // blue
    PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/format/sintel.webm").build();
    playerBlue.connect(webRtcEndpoint);
    playerBlue.play();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // ball
    PlayerEndpoint playerBall = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/format/rabbit.mov").build();
    playerBall.connect(webRtcEndpoint);
    playerBall.play();
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME) / N_PLAYER);

    // rtsp
    PlayerEndpoint playerRtsp =
        new PlayerEndpoint.Builder(mp, "rtsp://195.55.223.100/axis-media/media.amp").build();
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
