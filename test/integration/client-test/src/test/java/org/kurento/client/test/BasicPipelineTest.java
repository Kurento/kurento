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
import org.junit.Test;
import org.kurento.client.HttpPostEndpoint;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.MediaPipelineBaseTest;

public class BasicPipelineTest extends MediaPipelineBaseTest {

  @Test
  public void basicPipelineTest() {

    PlayerEndpoint player =
        new PlayerEndpoint.Builder(pipeline, "http://" + getTestFilesHttpPath()
            + "/video/format/small.webm").build();

    HttpPostEndpoint httpEndpoint = new HttpPostEndpoint.Builder(pipeline).build();

    player.connect(httpEndpoint);

    for (int i = 0; i < 100; i++) {

      WebRtcEndpoint webRtc = new WebRtcEndpoint.Builder(pipeline).build();

      player.connect(webRtc);

    }

    System.out.println("Dot length: " + pipeline.getGstreamerDot().getBytes().length);

    String url = httpEndpoint.getUrl();

    player.release();

    Assert.assertNotSame("The URL shouldn't be empty", "", url);
  }

}
