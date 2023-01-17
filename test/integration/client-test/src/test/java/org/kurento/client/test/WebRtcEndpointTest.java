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

import org.junit.Before;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.SdpBaseTest;

/**
 * {@link WebRtcEndpoint} test suite.
 *
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class WebRtcEndpointTest extends SdpBaseTest<WebRtcEndpoint> {

  @Before
  public void setupMediaElements() {
    sdp = new WebRtcEndpoint.Builder(pipeline).build();
    sdp2 = new WebRtcEndpoint.Builder(pipeline).build();

  }

}
