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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EventListener;
import org.kurento.client.MediaFlowInStateChangeEvent;
import org.kurento.client.MediaFlowOutStateChangeEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.IPVMode;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * WebRTC in loopback. <br>
 *
 * Media Pipeline(s): <br>
 * · WebRtcEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * IvpX Modes: <br>
 * · IPV4 <br>
 * · IPV6 <br>
 *
 * Media Modes: <br>
 * · Audio/Video <br>
 * · Only Video <br>
 * · Only Audio <br>
 *
 * Connection Modes: <br>
 * · Send/Recv <br>
 * · Send Only <br>
 * · Receive Only <br>
 *
 * Test logic: <br>
 * 1. (KMS) WebRtcEndpoint in loopback <br>
 * 2. (Browser) WebRtcPeer in different mode sends and receives media <br>
 * 3. (Browser) WebRtcPeer filters candidates according with IPVmode (IPV4, IPV6) Main assertion(s): <br>
 * · The event CONNECTED arrives <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */

public class WebRtcIceTest extends FunctionalTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTestSendRecv(WebRtcChannel webRtcChannel, IPVMode ipvMode)
      throws InterruptedException {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEndpoint
        .addMediaFlowOutStateChangeListener(new EventListener<MediaFlowOutStateChangeEvent>() {

          @Override
          public void onEvent(MediaFlowOutStateChangeEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
            }
          }
        });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, webRtcChannel, WebRtcMode.SEND_RCV, ipvMode);

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    Assert.assertTrue("Not received FLOWING OUT event in webRtcEp:" + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // Release Media Pipeline
    mp.release();
  }

  private void initTestRcvOnly(WebRtcChannel webRtcChannel, IPVMode ipvMode, String nameMedia)
      throws InterruptedException {

    String mediaUrl = getMediaUrl(Protocol.HTTP, nameMedia);
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEp.addMediaFlowInStateChangeListener(new EventListener<MediaFlowInStateChangeEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangeEvent event) {
        if (event.getState().equals(MediaFlowState.FLOWING)) {
          eosLatch.countDown();
        }
      }
    });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, webRtcChannel, WebRtcMode.RCV_ONLY, ipvMode);
    playerEp.play();

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event): " + mediaUrl + " "
        + webRtcChannel, getPage().waitForEvent("playing"));

    Assert.assertTrue("Not received FLOWING IN event in webRtcEp: " + mediaUrl + " "
        + webRtcChannel, eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // Release Media Pipeline
    mp.release();
  }

  private void initTestSendOnly(WebRtcChannel webRtcChannel, IPVMode ipvMode)
      throws InterruptedException {

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEpSendOnly = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEpRcvOnly = new WebRtcEndpoint.Builder(mp).build();

    webRtcEpSendOnly.connect(webRtcEpRcvOnly);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEpRcvOnly
        .addMediaFlowInStateChangeListener(new EventListener<MediaFlowInStateChangeEvent>() {

          @Override
          public void onEvent(MediaFlowInStateChangeEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
            }
          }
        });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEpSendOnly, webRtcChannel, WebRtcMode.SEND_ONLY, ipvMode);
    getPage().initWebRtc(webRtcEpRcvOnly, webRtcChannel, WebRtcMode.RCV_ONLY, ipvMode);

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    Assert.assertTrue("Not received FLOWING IN event in webRtcEpRcvOnly: " + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // Release Media Pipeline
    mp.release();
  }

  @Test
  public void testWebRtcHostIpv4SendRcvAudioVideo() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_AND_VIDEO, IPVMode.IPV4);
  }

  @Test
  public void testWebRtcHostIpv4SendRcvAudioOnly() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_ONLY, IPVMode.IPV4);
  }

  @Test
  public void testWebRtcHostIpv4SendRcvVideoOnly() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.VIDEO_ONLY, IPVMode.IPV4);
  }

  @Ignore
  public void testWebRtcHostIpv4SendOnlyAudioVideo() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_AND_VIDEO, IPVMode.IPV4);
  }

  @Ignore
  public void testWebRtcHostIpv4SendOnlyAudioOnly() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_ONLY, IPVMode.IPV4);
  }

  @Ignore
  public void testWebRtcHostIpv4SendOnlyVideoOnly() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.VIDEO_ONLY, IPVMode.IPV4);
  }

  @Test
  public void testWebRtcHostIpv4RcvOnlyAudioVideo() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_AND_VIDEO, IPVMode.IPV4, "/video/15sec/rgb.webm");
  }

  @Test
  public void testWebRtcHostIpv4RcvOnlyAudioOnly() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_ONLY, IPVMode.IPV4, "/audio/10sec/cinema.ogg");
  }

  @Test
  public void testWebRtcHostIpv4RcvOnlyVideoOnly() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.VIDEO_ONLY, IPVMode.IPV4, "/video/15sec/rgb.webm");
  }

  @Test
  public void testWebRtcHostIpv6SendRcvAudioVideo() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_AND_VIDEO, IPVMode.IPV6);
  }

  @Test
  public void testWebRtcHostIpv6SendRcvAudioOnly() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_ONLY, IPVMode.IPV6);
  }

  @Test
  public void testWebRtcHostIpv6SendRcvVideoOnly() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.VIDEO_ONLY, IPVMode.IPV6);
  }

  @Ignore
  public void testWebRtcHostIpv6SendOnlyAudioVideo() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_AND_VIDEO, IPVMode.IPV6);
  }

  @Ignore
  public void testWebRtcHostIpv6SendOnlyAudioOnly() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_ONLY, IPVMode.IPV6);
  }

  @Ignore
  public void testWebRtcHostIpv6SendOnlyVideoOnly() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.VIDEO_ONLY, IPVMode.IPV6);
  }

  @Test
  public void testWebRtcHostIpv6RcvOnlyAudioVideo() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_AND_VIDEO, IPVMode.IPV6, "/video/15sec/rgb.webm");
  }

  @Test
  public void testWebRtcHostIpv6RcvOnlyAudioOnly() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_ONLY, IPVMode.IPV6, "/audio/10sec/cinema.ogg");
  }

  @Test
  public void testWebRtcHostIpv6RcvOnlyVideoOnly() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.VIDEO_ONLY, IPVMode.IPV6, "/video/15sec/rgb.webm");
  }
}
