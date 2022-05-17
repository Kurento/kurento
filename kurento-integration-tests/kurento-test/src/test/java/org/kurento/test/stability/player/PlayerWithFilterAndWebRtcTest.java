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

package org.kurento.test.stability.player;

import java.awt.Color;
import java.util.Collection;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaFlowInStateChangedEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.utils.CheckAudioTimerTask;

/**
 * Test player stability.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> FaceOverlayFilter -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) During the playback of a stream from a PlayerEndpoint to a FaceOverlayFilter and this
 * to a WebRtcEndpoint, the FaceOverlayFilter is destroyed and created many times. After creating
 * FaceOverlayFilter, PlayerEndpoint is connected to FaceOverlayFilter and this is connected to
 * WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color of the video should remain when a filter is created and connected again</li>
 * <li>Audio of the video should remain when a filter is created and connected again</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * </ul>
 *
 * @author Raul Benitez (raulbenitezmejias@gmail.com)
 * @since 6.5.1
 */
public class PlayerWithFilterAndWebRtcTest extends StabilityTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testPlayerWithFilterAndWebRtcHD() throws Exception {
    String mediaUrl = "http://" + getTestFilesHttpPath() + "/video/15sec/rgbHD.webm";
    Color expectedColor = Color.RED;
    doTest(mediaUrl, expectedColor);
  }

  @Test
  public void testPlayerWithFilterAndWebRtcSD() throws Exception {
    String mediaUrl = "http://" + getTestFilesHttpPath() + "/video/15sec/rgb.webm";
    Color expectedColor = Color.RED;
    doTest(mediaUrl, expectedColor);
  }

  private void doTest(String mediaUrl, Color expectedColor) throws Exception {
    // Test data
    Timer gettingStats = new Timer();
    final CountDownLatch errorContinuityAudiolatch = new CountDownLatch(1);

    final int playTimeSeconds = 3;
    final int numRepeat = 200;
    final CountDownLatch flowingLatch = new CountDownLatch(1);
    final CountDownLatch eosLatch = new CountDownLatch(1);

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();

    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        log.debug("Received EndOfStream Event");
        eosLatch.countDown();
      }
    });

    FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(filter);
    filter.connect(webRtcEp);

    // WebRTC in receive-only mode
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
    playerEp.play();

    webRtcEp.addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangedEvent event) {
        if (event.getState().equals(MediaFlowState.FLOWING)) {
          if (flowingLatch.getCount() != 0) {
            flowingLatch.countDown();
          }
        }
      }
    });

    Assert.assertTrue("Not received FLOWING IN event in webRtcEp: " + mediaUrl,
        flowingLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    getPage().activatePeerConnectionInboundStats("webRtcPeer.peerConnection");

    gettingStats.schedule(new CheckAudioTimerTask(errorContinuityAudiolatch, getPage()), 100, 200);
    for (int i = 0; i < numRepeat; i++) {
      playerEp.setPosition(0);
      Assert.assertTrue("The color of the video should be " + expectedColor,
          getPage().similarColor(expectedColor));

      Thread.sleep(TimeUnit.SECONDS.toMillis(playTimeSeconds));

      Assert.assertTrue("Check audio. There were more than 2 seconds without receiving packets",
          errorContinuityAudiolatch.getCount() == 1);

      filter.release();
      filter = new FaceOverlayFilter.Builder(mp).build();
      playerEp.connect(filter);
      filter.connect(webRtcEp);
      if (eosLatch.getCount() == 0) {
        playerEp.play();
        Thread.sleep(1000);
      }
    }

    gettingStats.cancel();

    // Release Media Pipeline
    mp.release();
  }

}
