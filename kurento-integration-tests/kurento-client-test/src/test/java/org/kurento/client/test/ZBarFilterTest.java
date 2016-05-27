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
import org.kurento.client.CodeFoundEvent;
import org.kurento.client.EventListener;
import org.kurento.client.HttpEndpoint;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.ZBarFilter;
import org.kurento.client.test.util.AsyncEventManager;
import org.kurento.client.test.util.MediaPipelineBaseTest;

/**
 * {@link HttpEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link HttpEndpoint#getUrl()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link HttpEndpoint#addMediaSessionStartListener(EventListener)}
 * <li>{@link HttpEndpoint#addMediaSessionTerminatedListener(EventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class ZBarFilterTest extends MediaPipelineBaseTest {

  private ZBarFilter zbar;

  @Before
  public void setupMediaElements() {
    zbar = new ZBarFilter.Builder(pipeline).build();
  }

  @After
  public void teardownMediaElements() {
    zbar.release();
  }

  @Test
  public void testCodeFoundEvent() throws InterruptedException {

    PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, URL_BARCODES).build();
    player.connect(zbar);

    AsyncEventManager<CodeFoundEvent> async = new AsyncEventManager<>("CodeFound event");

    zbar.addCodeFoundListener(async.getMediaEventListener());

    player.play();

    async.waitForResult();

    player.stop();
    player.release();
  }

}
