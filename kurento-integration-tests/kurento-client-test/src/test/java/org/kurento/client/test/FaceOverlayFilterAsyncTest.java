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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.AsyncResultManager;

/**
 * {@link FaceOverlayFilter} test suite.
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 *
 */
public class FaceOverlayFilterAsyncTest extends MediaPipelineAsyncBaseTest {

  private PlayerEndpoint player;

  private FaceOverlayFilter overlayFilter;

  @Before
  public void setupMediaElements() throws InterruptedException {

    player = new PlayerEndpoint.Builder(pipeline, URL_POINTER_DETECTOR).build();

    AsyncResultManager<FaceOverlayFilter> async =
        new AsyncResultManager<>("FaceOverlayFilter creation");

    new FaceOverlayFilter.Builder(pipeline).buildAsync(async.getContinuation());

    overlayFilter = async.waitForResult();
  }

  @After
  public void teardownMediaElements() throws InterruptedException {
    player.release();
    releaseMediaObject(overlayFilter);
  }

  /**
   * Test if a {@link FaceOverlayFilter} can be created in the KMS. The filter is pipelined with a
   * {@link PlayerEndpoint}, which feeds video to the filter. This test depends on the correct
   * behaviour of the player and its events.
   *
   * @throws InterruptedException
   */
  @Test
  public void testFaceOverlayFilter() throws InterruptedException {

    player.connect(overlayFilter);

    AsyncEventManager<EndOfStreamEvent> async = new AsyncEventManager<>("EndOfStream event");

    player.addEndOfStreamListener(async.getMediaEventListener());

    player.play();

    async.waitForResult();
  }

}