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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.MediaPipelineBaseTest;
import org.kurento.commons.exception.KurentoException;

/**
 * {@link PlayerEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link PlayerEndpoint#getUri()}
 * <li>{@link PlayerEndpoint#play()}
 * <li>{@link PlayerEndpoint#pause()}
 * <li>{@link PlayerEndpoint#stop()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link PlayerEndpoint#addEndOfStreamListener(EventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class PlayerEndpointTest extends MediaPipelineBaseTest {

  private PlayerEndpoint player;

  @Before
  public void setupMediaElements() throws KurentoException {
    player = new PlayerEndpoint.Builder(pipeline, URL_SMALL).build();

  }

  @After
  public void teardownMediaElements() {
    if (player != null) {
      player.release();
    }
  }

  /**
   * start/pause/stop sequence test
   */
  @Test
  public void testPlayer() {
    player.play();
    player.pause();
    player.stop();
  }

  @Test
  public void testEventEndOfStream() throws InterruptedException {

    AsyncEventManager<EndOfStreamEvent> async = new AsyncEventManager<>("EndOfStream event");

    player.addEndOfStreamListener(async.getMediaEventListener());

    player.play();

    async.waitForResult();
  }

  @Test
  public void testCommandGetUri() {
    Assert.assertTrue(URL_SMALL.equals(player.getUri()));
  }

}
