/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.client.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.MediaType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.client.test.util.SdpAsyncBaseTest;

/**
 * {@link RtpEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RtpEndpoint#getLocalSessionDescriptor()}
 * <li>{@link RtpEndpoint#getRemoteSessionDescriptor()}
 * <li>{@link RtpEndpoint#generateOffer()}
 * <li>{@link RtpEndpoint#processOffer(String)}
 * <li>{@link RtpEndpoint#processAnswer(String)}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link RtpEndpoint#addMediaSessionStartedListener(EventListener)}
 * <li>{@link RtpEndpoint#addMediaSessionTerminatedListener(EventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class RtpEndpointAsyncTest extends SdpAsyncBaseTest<RtpEndpoint> {

  @Before
  public void setupMediaElements() throws InterruptedException {

    AsyncResultManager<RtpEndpoint> async = new AsyncResultManager<>("RtpEndpoint creation");
    new RtpEndpoint.Builder(pipeline).buildAsync(async.getContinuation());
    sdp = async.waitForResult();
    Assert.assertNotNull(sdp);

    AsyncResultManager<RtpEndpoint> async2 = new AsyncResultManager<>("RtpEndpoint creation");
    new RtpEndpoint.Builder(pipeline).buildAsync(async2.getContinuation());
    sdp2 = async2.waitForResult();
    Assert.assertNotNull(sdp2);
  }

  @Test
  public void testRtpEndpointSimulatingAndroidSdp() throws InterruptedException {

    PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, URL_BARCODES).build();

    RtpEndpoint rtpEndpoint = new RtpEndpoint.Builder(pipeline).build();

    String requestSdp = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n"
        + "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n" + "m=video 52126 RTP/AVP 96 97 98\r\n"
        + "a=rtpmap:96 H264/90000\r\n" + "a=rtpmap:97 MP4V-ES/90000\r\n"
        + "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n" + "b=AS:384\r\n";

    rtpEndpoint.processOffer(requestSdp);
    player.connect(rtpEndpoint, MediaType.VIDEO);
    player.play();

    // just a little bit of time before destroying
    Thread.sleep(2000);
  }

}