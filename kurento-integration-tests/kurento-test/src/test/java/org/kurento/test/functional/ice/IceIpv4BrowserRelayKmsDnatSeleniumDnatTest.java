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

package org.kurento.test.functional.ice;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcCandidateType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcIpvMode;
import org.kurento.test.config.TestScenario;

/**
 *
 * Parameters: <br>
 * -Dtest.kms.dnat=true <br>
 * -Dtest.kms.transport=UDP <br>
 * -Dtest.selenium.dnat=true <br>
 * -Dtest.selenium.transport=UDP
 *
 * WebRTC in loopback. <br>
 *
 * Media Pipeline(s): <br>
 * · WebRtcEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Candidate Type: <br>
 * · RELAY <br>
 *
 * IvpX Modes: <br>
 * · IPV4 <br>
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
 * 3. (Browser) WebRtcPeer filters candidates according with IPVmode and CandidateType Main
 * assertion(s): <br>
 * · The event CONNECTED arrives <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */

public class IceIpv4BrowserRelayKmsDnatSeleniumDnatTest extends SimpleIceTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromesAndFirefoxs(2);
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatSendRcvAudioVideo()
      throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_AND_VIDEO, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY);
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatSendRcvAudioOnly()
      throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_ONLY, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY);
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatSendRcvVideoOnly()
      throws InterruptedException {
    initTestSendRecv(WebRtcChannel.VIDEO_ONLY, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY);
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatSendOnlyAudioVideo()
      throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_AND_VIDEO, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY);
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatSendOnlyAudioOnly()
      throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_ONLY, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY);
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatSendOnlyVideoOnly()
      throws InterruptedException {
    initTestSendOnly(WebRtcChannel.VIDEO_ONLY, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY);
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatRcvOnlyAudioVideo()
      throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_AND_VIDEO, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY,
        "/video/15sec/rgb.webm");
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatRcvOnlyAudioOnly()
      throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_ONLY, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY,
        "/audio/10sec/cinema.ogg");
  }

  @Test
  public void testIceIpv4BrowserRelayKmsDnatSeleniumDnatRcvOnlyVideoOnly()
      throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.VIDEO_ONLY, WebRtcIpvMode.IPV4, WebRtcCandidateType.RELAY,
        "/video/15sec/rgb.webm");
  }
}
