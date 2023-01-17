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
import org.kurento.client.EventListener;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.client.test.util.SdpAsyncBaseTest;

/**
 * {@link WebRtcEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#getLocalSessionDescriptor()}
 * <li>{@link WebRtcEndpoint#getRemoteSessionDescriptor()}
 * <li>{@link WebRtcEndpoint#generateOffer()}
 * <li>{@link WebRtcEndpoint#processOffer(String)}
 * <li>{@link WebRtcEndpoint#processAnswer(String)}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#addMediaSessionStartedListener(EventListener)}
 * <li>{@link WebRtcEndpoint#addMediaSessionTerminatedListener(EventListener)}
 * </ul>
 *
 *
 * @author Jose Antonio Santos Cadenas (santoscadenas@gmail.com)
 * @version 1.0.0
 *
 */
public class WebRtcEndpointAsyncTest extends SdpAsyncBaseTest<WebRtcEndpoint> {

  @Before
  public void setupMediaElements() throws InterruptedException {

    AsyncResultManager<WebRtcEndpoint> async = new AsyncResultManager<>("RtpEndpoint creation");
    new WebRtcEndpoint.Builder(pipeline).buildAsync(async.getContinuation());
    sdp = async.waitForResult();
    Assert.assertNotNull(sdp);

    AsyncResultManager<WebRtcEndpoint> async2 = new AsyncResultManager<>("RtpEndpoint creation");
    new WebRtcEndpoint.Builder(pipeline).buildAsync(async2.getContinuation());
    sdp2 = async2.waitForResult();
    Assert.assertNotNull(sdp2);
  }

}