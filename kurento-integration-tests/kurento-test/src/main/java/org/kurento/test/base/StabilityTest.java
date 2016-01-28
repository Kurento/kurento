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

package org.kurento.test.base;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.testing.SystemStabilityTests;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.browser.WebRtcTestPage;

/**
 * Stability tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
@Category(SystemStabilityTests.class)
public class StabilityTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  public StabilityTest() {
    setDeleteLogsIfSuccess(false);
  }

  public void testPlayerMultipleSeek(String mediaUrl, WebRtcChannel webRtcChannel,
      int pauseTimeSeconds, int numSeeks, Map<Integer, Color> expectedPositionAndColor)
      throws Exception {
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
    playerEP.connect(webRtcEP);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        log.debug("Received EndOfStream Event");
        eosLatch.countDown();
      }
    });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEP, webRtcChannel, WebRtcMode.RCV_ONLY);
    playerEP.play();

    // TODO: Check with playerEP.getVideoInfo().getIsSeekable() if the video is seekable. If not,
    // assert with exception from KMS

    Thread.sleep(TimeUnit.SECONDS.toMillis(pauseTimeSeconds));
    for (int i = 0; i < numSeeks; i++) {
      log.debug("Try to set position in 0");
      playerEP.setPosition(0);
      for (Integer position : expectedPositionAndColor.keySet()) {
        log.debug("Try to set position in {}", position);
        playerEP.setPosition(position);
        if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
          Assert.assertTrue("After set position to " + position
              + "ms, the color of the video should be " + expectedPositionAndColor.get(position),
              getPage().similarColor(expectedPositionAndColor.get(position)));
        }
        // TODO: Add new method for checking that audio did pause properly when kurento-utils has
        // the
        // feature.
      }
    }

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event): " + mediaUrl + " "
        + webRtcChannel, getPage().waitForEvent("playing"));

    Assert.assertTrue("Not received EOS event in player: " + mediaUrl + " " + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // Release Media Pipeline
    mp.release();
  }
}
