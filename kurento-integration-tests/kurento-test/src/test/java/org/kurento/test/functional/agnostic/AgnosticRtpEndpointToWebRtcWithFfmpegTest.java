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

package org.kurento.test.functional.agnostic;

import static org.kurento.test.config.Protocol.FILE;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Continuation;
import org.kurento.client.CryptoSuite;
import org.kurento.client.EventListener;
import org.kurento.client.MediaFlowInStateChangedEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.SDES;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.utils.Shell;

/**
 * Test agnostic.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>RtpEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Create a RtpEndpoint and connect to WebRtcEndpoint</li>
 * <li>Use a specific sdp for RtpEndpoint</li>
 * <li>Get the port that processOffer returns
 * <li>Run a ffmpeg command for starting the media
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color of the video should remain when a filter is created and connected again</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * </ul>
 *
 * @author Raul Benitez (raulbenitezmejias@gmail.com)
 * @since 6.5.1
 */
public class AgnosticRtpEndpointToWebRtcWithFfmpegTest extends FunctionalTest {

  private String port = "";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void agnosticRtpEndpointToWebRtcWithFfmpeg() throws Exception {

    final CountDownLatch proccessOfferLatch = new CountDownLatch(1);

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    SDES sdes = new SDES();
    sdes.setCrypto(CryptoSuite.AES_128_CM_HMAC_SHA1_80);
    RtpEndpoint rtpEp = new RtpEndpoint.Builder(mp).withCrypto(sdes).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();

    final CountDownLatch flowingInLatch = new CountDownLatch(1);

    webRtcEp.addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangedEvent event) {
        flowingInLatch.countDown();
      }
    });

    rtpEp.connect(webRtcEp);

    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    String sdp = "v=0\r\n" + "o=- 0 0 IN IP4 0.0.0.0\r\n" + "s=-\r\n" + "t=0 0\r\n"
        + "m=video 1 RTP/SAVP 96\r\n" + "c=IN IP4 0.0.0.0\r\n" + "a=rtpmap:96 H264/90000\r\n"
        + "a=fmtp:96 packetization-mode=1\r\n"
        + "a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:ZDJ4Ump3N0xtRTg0V0k4OWJNaXpKRFl3ejZ0QVJW";

    rtpEp.processOffer(sdp, new Continuation<String>() {

      @Override
      public void onSuccess(String result) throws Exception {
        String[] parse = result.split("m=video");
        String port_ = parse[1].split(" ")[1];
        port = port_;
        proccessOfferLatch.countDown();
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        log.debug("Error:{}", cause.getMessage());
      }
    });

    proccessOfferLatch.await(getPage().getTimeout(), TimeUnit.SECONDS);

    String[] kmsUriParse = kms.getWsUri().split("//");
    String kmsIp = kmsUriParse[1].split(":")[0];

    String mediaPath = FILE + "://" + getTestFilesDiskPath() + "/video/30sec/rgb.mp4";
    String ffmpegCmd = "ffmpeg -re -i " + mediaPath
        + " -an -vcodec libx264 -profile:v baseline -level 3.0 -f rtp -srtp_out_suite AES_CM_128_HMAC_SHA1_80 -srtp_out_params ZDJ4Ump3N0xtRTg0V0k4OWJNaXpKRFl3ejZ0QVJW srtp://"
        + kmsIp + ":" + port;

    log.debug("Media Path: {}", mediaPath);
    log.debug("Uri: {}:{}", kmsIp, port);
    log.debug("Ffmpeg cmd: {}", ffmpegCmd);

    Shell.run(ffmpegCmd.split(" "));

    Assert.assertTrue("Not received FLOWING IN event in webRtcEp:",
        flowingInLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    Color[] expectedColors = { Color.RED, Color.GREEN, Color.BLUE };
    for (Color expectedColor : expectedColors) {
      Assert.assertTrue("The color of the video should be " + expectedColor,
          getPage().similarColor(expectedColor));
    }

    mp.release();
  }
}
