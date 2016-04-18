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

/**
 * Stability tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
@Category(SystemStabilityTests.class)
public class StabilityTest extends RepositoryMongoTest {

  public final long DEFAULT_TEST_DURATION = 300000; // ms

  public long endTestTime = 0;

  public StabilityTest() {
    setDeleteLogsIfSuccess(false);
  }

  public boolean isTimeToFinishTest() {
    return System.currentTimeMillis() > endTestTime;
  }

  public void testPlayerMultipleSeek(String mediaUrl, WebRtcChannel webRtcChannel,
      int pauseTimeSeconds, int numSeeks, Map<Integer, Color> expectedPositionAndColor)
          throws Exception {
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        log.debug("Received EndOfStream Event");
        eosLatch.countDown();
      }
    });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, webRtcChannel, WebRtcMode.RCV_ONLY);
    playerEp.play();

    Assert.assertTrue("Not received media (timeout waiting playing event): " + mediaUrl + " "
        + webRtcChannel, getPage().waitForEvent("playing"));

    // TODO: Check with playerEP.getVideoInfo().getIsSeekable() if the video is seekable. If not,
    // assert with exception from KMS

    Thread.sleep(TimeUnit.SECONDS.toMillis(pauseTimeSeconds));
    Integer executions = -1;
    for (int i = 0; i < numSeeks; i++) {
      executions++;
      log.debug("Try to set position in 0");
      playerEp.setPosition(0);
      for (Integer position : expectedPositionAndColor.keySet()) {
        executions++;
        log.debug("Try to set position in {}", position);
        playerEp.setPosition(position);
        if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
          boolean isSimilarColor = getPage().similarColor(expectedPositionAndColor.get(position));
          log.debug("Is the color of the video: {} ? {} ", expectedPositionAndColor.get(position),
              isSimilarColor);
          if (eosLatch.getCount() == 0) {
            break;
          }
          Assert.assertTrue("After set position to " + position
              + "ms, the color of the video should be " + expectedPositionAndColor.get(position),
              isSimilarColor);
        }
        // TODO: Add new method for checking that audio did pause properly when kurento-utils has
        // the
        // feature.
      }
      if (eosLatch.getCount() == 0) {
        break;
      }
    }

    Integer executionsExpected = (numSeeks * expectedPositionAndColor.size()) + numSeeks - 1;

    log.info("The times executed. Expected  {}. Total {}.", executionsExpected, executions);
    Assert.assertTrue("The times executed is wrong. Minimun should be 1. Total: " + executions,
        (executions > 1));

    // Assertions

    Assert.assertTrue("Not received EOS event in player: " + mediaUrl + " " + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // Release Media Pipeline
    playerEp.release();
    mp.release();
  }
}
