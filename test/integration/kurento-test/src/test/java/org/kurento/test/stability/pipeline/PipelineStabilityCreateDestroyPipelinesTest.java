/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.stability.pipeline;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ObjectCreatedEvent;
import org.kurento.client.ObjectDestroyedEvent;

/**
 * Stability test for Pipelines. <br/>
 * Test logic: <br/>
 * The logic is executed twice, the first is to stabilize the memory on the KMS, and in the second
 * the memory is checked.
 * <ol>
 * <li>N grows exponentially in base 2</li>
 * <li>Create N pipelines.</li>
 * <li>Release N pipelines</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>N Object_Created are received</li>
 * <li>N Object_Destroyed are received</li>
 * <li>The % of the memory is between 0 and 10.
 * </ul>
 *
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */
public class PipelineStabilityCreateDestroyPipelinesTest extends BasePipeline {
  private int INCREASE_EXPONENTIAL = 2;
  private int ITERATIONS = 13;
  private ObjectsLatch objectsLatch;

  @Test
  public void testCreateDestroyPipelines() throws Exception {
    doTest(false);
    doTest(true);
  }

  /**
   *
   * @param checkMemory
   *          Activate the memory assert.
   * @throws Exception
   */
  private void doTest(boolean checkMemory) throws Exception {

    initMemory();

    ListenerSubscription listenerObjectCreated =
        getServerManager().addObjectCreatedListener(new EventListener<ObjectCreatedEvent>() {

          @Override
          public void onEvent(ObjectCreatedEvent event) {
            objectsLatch.getObjectsCreatedLatch().countDown();
          }
        });

    ListenerSubscription listenerObjectDestroyed =
        getServerManager().addObjectDestroyedListener(new EventListener<ObjectDestroyedEvent>() {

          @Override
          public void onEvent(ObjectDestroyedEvent event) {
            objectsLatch.getObjectsDestroyedLatch().countDown();
          }
        });

    int objectsToCreate = 0;

    for (int i = 1; i <= ITERATIONS; i++) {
      objectsToCreate = (int) Math.pow(INCREASE_EXPONENTIAL, i);

      log.debug("Create {} MediaPipelines", objectsToCreate);

      ArrayList<MediaPipeline> mediaPipelines = new ArrayList<MediaPipeline>();

      objectsLatch = new ObjectsLatch(objectsToCreate);
      for (int j = 0; j < objectsToCreate; j++) {
        MediaPipeline mp = kurentoClient.createMediaPipeline();
        mediaPipelines.add(mp);
      }

      // Wait to all pipelines are created
      Assert.assertTrue(
          "The Objects are not created properly. Expected: " + objectsToCreate + ". No received "
              + (objectsToCreate - objectsLatch.getObjectsCreatedLatch().getCount())
              + " ObjectCreated event(s)",
          objectsLatch.getObjectsCreatedLatch().await(TIMEOUT, TimeUnit.SECONDS));

      // Release each MediaPipeline
      for (MediaPipeline pipeline : mediaPipelines) {
        pipeline.release();
      }

      Assert.assertTrue(
          "The Objects are not destroyed properly. Expected: " + objectsToCreate + ". No received "
              + (objectsToCreate - objectsLatch.getObjectsDestroyedLatch().getCount())
              + " ObjectDestroyed event(s)",
          objectsLatch.getObjectsDestroyedLatch().await(TIMEOUT, TimeUnit.SECONDS));

      // Verify the memory
      double percentageMemory = getMemoryIncrease();
      if (checkMemory) {
        Assert.assertTrue(
            "The memory increases more than 0%. The percentage memory was " + percentageMemory,
            percentageMemory >= 0.0 && percentageMemory <= 10.0);
      }
    }

    getServerManager().removeObjectCreatedListener(listenerObjectCreated);
    getServerManager().removeObjectDestroyedListener(listenerObjectDestroyed);
  }

}
