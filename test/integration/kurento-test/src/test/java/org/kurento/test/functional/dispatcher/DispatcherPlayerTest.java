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

package org.kurento.test.functional.dispatcher;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Dispatcher;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * A PlayerEndpoint is connected to a WebRtcEndpoint through a Dispatcher
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>2xPlayerEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Media server switchs the media from two PlayerEndpoint using a Dispatcher, streaming
 * the result through a WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>The color of the received video should be as expected (red and the blue)</li>
 * <li>EOS event should arrive to player</li>
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
public class DispatcherPlayerTest extends FunctionalTest {

  private static final int PLAYTIME = 10; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testDispatcherPlayer() throws Exception {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    PlayerEndpoint playerEp =
        new PlayerEndpoint.Builder(mp, "http://" + getTestFilesHttpPath() + "/video/10sec/red.webm")
            .build();
    PlayerEndpoint playerEp2 = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/10sec/blue.webm").build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();

    Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
    HubPort hubPort2 = new HubPort.Builder(dispatcher).build();
    HubPort hubPort3 = new HubPort.Builder(dispatcher).build();

    playerEp.connect(hubPort1);
    playerEp2.connect(hubPort3);
    hubPort2.connect(webRtcEp);
    dispatcher.connect(hubPort1, hubPort2);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
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
    Assert.assertTrue("The color of the video should be red", getPage().similarColor(Color.RED));

    Thread.sleep(5000);
    playerEp2.play();
    dispatcher.connect(hubPort3, hubPort2);
    Assert.assertTrue("The color of the video should be blue", getPage().similarColor(Color.BLUE));

    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
    double currentTime = getPage().getCurrentTime();
    Assert.assertTrue(
        "Error in play time (expected: " + PLAYTIME + " sec, real: " + currentTime + " sec)",
        getPage().compare(PLAYTIME, currentTime));

    // Release Media Pipeline
    mp.release();
  }
}
