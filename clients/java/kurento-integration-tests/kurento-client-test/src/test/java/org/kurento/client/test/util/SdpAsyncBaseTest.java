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

package org.kurento.client.test.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.SdpEndpoint;
import org.kurento.client.test.MediaPipelineAsyncBaseTest;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @param <T>
 *
 */
public abstract class SdpAsyncBaseTest<T extends SdpEndpoint> extends MediaPipelineAsyncBaseTest {

  protected T sdp;
  protected T sdp2;

  @After
  public void teardownMediaElements() throws InterruptedException {
    releaseMediaObject(sdp);
    releaseMediaObject(sdp2);
  }

  // TODO connect a local sdp or fails
  @Test
  public void testGetLocalSdpMethod() throws InterruptedException {

    AsyncResultManager<String> async =
        new AsyncResultManager<String>("sdp.generateOffer() invocation");
    sdp.generateOffer(async.getContinuation());
    async.waitForResult();

    AsyncResultManager<String> async2 =
        new AsyncResultManager<String>("sdp.getLocalSessionDescriptor() invocation");
    sdp.getLocalSessionDescriptor(async2.getContinuation());
    async2.waitForResult();
  }

  // TODO connect a remote sdp or fails
  @Test
  public void testGetRemoteSdpMethod() throws InterruptedException {

    String offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n"
        + "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n" + "m=video 52126 RTP/AVP 96 97 98\r\n"
        + "a=rtpmap:96 H264/90000\r\n" + "a=rtpmap:97 MP4V-ES/90000\r\n"
        + "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n" + "b=AS:384\r\n";

    AsyncResultManager<String> async =
        new AsyncResultManager<String>("sdp.processOffer() invocation");

    sdp.processOffer(offer, async.getContinuation());

    String result = async.waitForResult();

    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testGenerateSdpOfferMethod() {
    String offer = sdp.generateOffer();
    Assert.assertFalse(offer.isEmpty());
  }

  @Test
  public void testProcessOfferMethod() {
    String offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n"
        + "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n" + "m=video 52126 RTP/AVP 96 97 98\r\n"
        + "a=rtpmap:96 H264/90000\r\n" + "a=rtpmap:97 MP4V-ES/90000\r\n"
        + "a=rtpmap:98 H263-1998/90000\r\n" + "a=recvonly\r\n" + "b=AS:384\r\n";

    String ret = sdp.processOffer(offer);

    Assert.assertFalse(ret.isEmpty());
  }

  @Test
  public void testProcessAnswerMethod() {
    String offer = sdp.generateOffer();
    String answer = sdp2.processOffer(offer);

    String ret = sdp.processAnswer(answer);
    Assert.assertFalse(ret.isEmpty());
  }

}