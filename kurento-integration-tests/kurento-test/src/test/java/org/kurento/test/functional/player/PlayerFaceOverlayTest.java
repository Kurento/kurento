/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
 * Test of a PlayerEndpoint with a FaceOverlay. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> FaceOverlay -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) PlayerEndpoints streams media to FaceOverlay and then WebRtcEndpoint <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Image should be overlayed on the media stream (proper color should be detected) <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · EOS event should arrive to player <br>
 * · Play time in remote video should be as expected <br>
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
    final String mediaUrl = "http://files.kurento.org/video/filter/fiwarecut.mp4";
    final Color expectedColor = Color.RED;
    final int xExpectedColor = 420;
    final int yExpectedColor = 45;
    final String imgOverlayUrl = "http://files.kurento.org/img/red-square.png";
    final float offsetXPercent = -0.2F;
    final float offsetYPercent = -1.2F;
    final float widthPercent = 1.6F;
    final float heightPercent = 1.6F;

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
    FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp).build();
    filter.setOverlayedImage(imgOverlayUrl, offsetXPercent, offsetYPercent, widthPercent,
        heightPercent);
    playerEP.connect(filter);
    filter.connect(webRtcEP);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
    playerEP.play();

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
