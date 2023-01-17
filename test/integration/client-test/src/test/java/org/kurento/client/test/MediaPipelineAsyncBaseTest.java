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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.client.Continuation;
import org.kurento.client.MediaObject;
import org.kurento.client.MediaPipeline;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.testing.KurentoClientTests;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 *
 */
@Category(KurentoClientTests.class)
public abstract class MediaPipelineAsyncBaseTest extends ApiBase {

  protected MediaPipeline pipeline;

  @Before
  public void setupPipeline() throws InterruptedException {

    AsyncResultManager<MediaPipeline> async = new AsyncResultManager<>("MediaPipeline creation");

    kurentoClient.createMediaPipeline(async.getContinuation());

    pipeline = async.waitForResult();

    if (pipeline == null) {
      Assert.fail();
    }
  }

  @After
  public void teardownPipeline() throws InterruptedException {
    if (pipeline != null) {
      releaseMediaObject(pipeline);
    }
  }

  protected static void releaseMediaObject(final MediaObject mo) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    if (mo != null) {
      mo.release(new Continuation<Void>() {
        @Override
        public void onSuccess(Void result) {
          latch.countDown();
        }

        @Override
        public void onError(Throwable cause) {
          throw new KurentoException(cause);
        }
      });
      Assert.assertTrue("Timeout of 25s releasing object", latch.await(25, TimeUnit.SECONDS));
    }
  }

}