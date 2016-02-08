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

package org.kurento.test.functional.ice;

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcCandidateType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcIpvMode;
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
 * * Candidate Type: <br>
 * · HOST <br>
 *
 * IvpX Modes: <br>
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

public class IceHostIpv6Test extends SimpleIceTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testIceHostIpv6SendRcvAudioVideo() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_AND_VIDEO, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST);
  }

  @Test
  public void testIceHostIpv6SendRcvAudioOnly() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.AUDIO_ONLY, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST);
  }

  @Test
  public void testIceHostIpv6SendRcvVideoOnly() throws InterruptedException {
    initTestSendRecv(WebRtcChannel.VIDEO_ONLY, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST);
  }

  @Ignore
  public void testIceHostIpv6SendOnlyAudioVideo() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_AND_VIDEO, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST);
  }

  @Ignore
  public void testIceHostIpv6SendOnlyAudioOnly() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.AUDIO_ONLY, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST);
  }

  @Ignore
  public void testIceHostIpv6SendOnlyVideoOnly() throws InterruptedException {
    initTestSendOnly(WebRtcChannel.VIDEO_ONLY, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST);
  }

  @Test
  public void testIceHostIpv6RcvOnlyAudioVideo() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_AND_VIDEO, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST,
        "/video/15sec/rgb.webm");
  }

  @Test
  public void testIceHostIpv6RcvOnlyAudioOnly() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.AUDIO_ONLY, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST,
        "/audio/10sec/cinema.ogg");
  }

  @Test
  public void testIceHostIpv6RcvOnlyVideoOnly() throws InterruptedException {
    initTestRcvOnly(WebRtcChannel.VIDEO_ONLY, WebRtcIpvMode.IPV6, WebRtcCandidateType.HOST,
        "/video/15sec/rgb.webm");
  }
}
