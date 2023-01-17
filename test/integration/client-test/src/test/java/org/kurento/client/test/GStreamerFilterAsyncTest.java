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
import org.kurento.client.EventListener;
import org.kurento.client.GStreamerFilter;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.module.platedetector.PlateDetectorFilter;

/**
 * {@link PlateDetectorFilter} test suite.
 *
 * Events tested:
 * <ul>
 * <li>{@link PlateDetectorFilter#addPlateDetectedListener(EventListener)}
 * </ul>
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 2.0.1
 *
 */
public class GStreamerFilterAsyncTest extends MediaPipelineAsyncBaseTest {

  private GStreamerFilter filter;

  private PlayerEndpoint player;

  @Before
  public void setupMediaElements() {
    player = new PlayerEndpoint.Builder(pipeline, URL_PLATES).build();
  }

  @After
  public void teardownMediaElements() {
    player.release();
  }

  @Test
  public void testInstantiation() throws InterruptedException {

    AsyncResultManager<GStreamerFilter> async =
        new AsyncResultManager<GStreamerFilter>("GStreamerFilter creation");

    new GStreamerFilter.Builder(pipeline, "videoflip method=horizontal-flip")
        .buildAsync(async.getContinuation());

    filter = async.waitForResult();

    releaseMediaObject(filter);
  }

}