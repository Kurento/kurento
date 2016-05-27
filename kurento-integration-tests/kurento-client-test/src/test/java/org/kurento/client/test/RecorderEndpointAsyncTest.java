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
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.test.util.AsyncResultManager;

/**
 * {@link RecorderEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RecorderEndpoint#getUri()}
 * <li>{@link RecorderEndpoint#record()}
 * <li>{@link RecorderEndpoint#pause()}
 * <li>{@link RecorderEndpoint#stop()}
 * </ul>
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class RecorderEndpointAsyncTest extends MediaPipelineAsyncBaseTest {

  private RecorderEndpoint recorder;

  @Before
  public void setupMediaElements() throws InterruptedException {

    AsyncResultManager<RecorderEndpoint> async =
        new AsyncResultManager<>("RecorderEndpoint creation");

    new RecorderEndpoint.Builder(pipeline, URL_SMALL).buildAsync(async.getContinuation());

    recorder = async.waitForResult();

    Assert.assertNotNull(recorder);
  }

  @After
  public void teardownMediaElements() throws InterruptedException {
    releaseMediaObject(recorder);
  }

  @Test
  public void testGetUri() throws InterruptedException {

    AsyncResultManager<String> async = new AsyncResultManager<>("recorder.getUri() invocation");

    recorder.getUri(async.getContinuation());

    String uri = async.waitForResult();

    Assert.assertEquals(URL_SMALL, uri);
  }

  @Test
  public void testRecorder() throws InterruptedException {

    AsyncResultManager<Void> asyncRecord = new AsyncResultManager<>("recorder.record() invocation");
    recorder.record(asyncRecord.getContinuation());
    asyncRecord.waitForResult();

    AsyncResultManager<Void> asyncPause = new AsyncResultManager<>("recorder.pause() invocation");
    recorder.pause(asyncPause.getContinuation());
    asyncPause.waitForResult();

    AsyncResultManager<Void> asyncStop = new AsyncResultManager<>("recorder.stop() invocation");
    recorder.pause(asyncStop.getContinuation());
    asyncStop.waitForResult();

  }
}