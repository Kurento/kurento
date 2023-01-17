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

package org.kurento.test.functional.player;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Test of a PlayerEndpoint with a FaceOverlay.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> FaceOverlay -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) PlayerEndpoints streams media to FaceOverlay and then WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Image should be overlayed on the media stream (proper color should be detected)</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time in remote video should be as expected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class PlayerFaceOverlayTest extends FunctionalTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testPlayerFaceOverlay() throws Exception {
    // Test data
    final int playTimeSeconds = 30;
    final String mediaUrl = "http://" + getTestFilesHttpPath() + "/video/filter/fiwarecut.mp4";
    final Color expectedColor = Color.RED;
    final int xExpectedColor = 420;
    final int yExpectedColor = 45;
    final String imgOverlayUrl = "http://" + getTestFilesHttpPath() + "/img/red-square.png";
    final float offsetXPercent = -0.2F;
    final float offsetYPercent = -1.2F;
    final float widthPercent = 1.6F;
    final float heightPercent = 1.6F;

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp).build();
    filter.setOverlayedImage(imgOverlayUrl, offsetXPercent, offsetYPercent, widthPercent,
        heightPercent);
    playerEp.connect(filter);
    filter.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
    playerEp.play();

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));
    Assert.assertTrue(
        "Color at coordinates " + xExpectedColor + "," + yExpectedColor + " must be "
            + expectedColor,
        getPage().similarColorAt(expectedColor, xExpectedColor, yExpectedColor));
    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue(
        "Error in play time (expected: " + playTimeSeconds + " sec, real: " + currentTime + " sec)",
        getPage().compare(playTimeSeconds, currentTime));

    // Release Media Pipeline
    mp.release();
  }
}
