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
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.kurento.client.ElementConnectedEvent;
import org.kurento.client.ElementDisconnectedEvent;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ObjectCreatedEvent;
import org.kurento.client.ObjectDestroyedEvent;
import org.kurento.client.PassThrough;

/**
 * Stability test for Pipelines and PassThrough. Connect/Disconnect several Passthroughs like a
 * chain <br/>
 *
 * <pre>
 * Pipeline -> PassThrough -> PassThrough -> ... -> PassThrough
 * </pre>
 *
 * Test logic: <br/>
 * <ul>
 * <li>The test procedure will be repeated during i iterations</li>
 * <li>N is 2^i</li>
 * </ul>
 *
 * Test procedure: <br/>
 * <ol>
 * <li>Create M pipelines, it will be a constant value</li>
 * <li>Create N passThrough in each pipeline and connect the elements of each pipeline like a
 * chain</li>
 * <li>Disconnect N passThrough</li>
 * <li>Release all passThrough. Depends of the test, this point will be executed or not. See doTest
 * method</li>
 * <li>Release M pipelines</li>
 * </ol>
 *
 * Main assertion(s):
 * <ul>
 * <li>((M * N) + M) Object_Created are received</li>
 * <li>((M * N) + M) Object_Destroyed are received</li>
 * <li>(M * (N - 1)) * 3 Connected Events are received. Each pipeline will have N - 1 connections
 * because the last passthrough is not connected, and each connection launches 3 events (VIDEO,
 * AUDIO and DATA)</li>
 * <li>(M * (N -1)) * 3 Disconnected Events are received. Each pipeline will have N - 1 connections
 * because the last passthrough is not disconnected, and each disconnection launches 3 events
 * (VIDEO, AUDIO and DATA)</li>
 * <li>The % of the memory is between 0 and 10</li>
 * </ul>
 *
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */

@Ignore
public class PipelineStabilityConnectDisconnectPassthroughChainedTest extends BasePipeline {
  private int INCREASE_EXPONENTIAL = 2;
  private int ITERATIONS = 8;
  private int M = 4;
  private ObjectsLatch objectsLatch;
  private ConnectionStateLatch connectionStateLatch;

  @Ignore
  public void testConnectDisconnectPassthroughChainedOnePipelineDestroyPipeline() throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, false, false);
    // Use the assert for checking memory
    doTest(1, false, true);
  }

  @Ignore
  public void testConnectDisconnectPassthroughChainedOnePipelineDestroyEachPassThrough()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, true, false);
    // Use the assert for checking memory
    doTest(1, true, true);
  }

  @Ignore
  public void testConnectDisconnectPassthroughChainedMPipelineDestroyPipeline() throws Exception {
    // Stabilize the memory on the KMS
    doTest(M, false, false);
    // Use the assert for checking memory
    doTest(M, false, true);
  }

  @Ignore
  public void testConnectDisconnectPassthroughChainedMPipelineDestroyEachPassThrough()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(M, true, false);
    // Use the assert for checking memory
    doTest(M, true, true);
  }

  /**
   *
   * @param numPipelinesToCreate
   *          Define the number of the pipelines that will be created
   * @param destroyEachPassThrough
   *          Define if each passthrough will be released one by one
   * @param checkMemory
   *          Activate the memory assert.
   * @throws Exception
   */
  private void doTest(int numPipelinesToCreate, boolean destroyEachPassThrough, boolean checkMemory)
      throws Exception {

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

    int passthroughToCreate = 0;
    int objectsToCreate = 0;

    for (int i = 1; i <= ITERATIONS; i++) {
      HashMap<MediaPipeline, ArrayList<PassThrough>> mediaPipelineChildren =
          new HashMap<MediaPipeline, ArrayList<PassThrough>>();

      passthroughToCreate = (int) Math.pow(INCREASE_EXPONENTIAL, i);

      objectsToCreate = (passthroughToCreate * numPipelinesToCreate) + numPipelinesToCreate;

      log.debug(
          "Create {} MediaPipelines; Create {} Passthrough by MediaPipeline; Total: {} objects",
          numPipelinesToCreate, passthroughToCreate, objectsToCreate);

      ArrayList<PassThrough> allPassThroughs = new ArrayList<PassThrough>();
      ArrayList<MediaPipeline> allMediaPipelines = new ArrayList<MediaPipeline>();

      // Each pipeline will have (passthroughToCreate - 1) connections because one passthrough is
      // not connected, and each connection launches 3 events (VIDEO, AUDIO and DATA)
      int numConnectionEvents = ((passthroughToCreate - 1) * numPipelinesToCreate) * 3;
      connectionStateLatch = new ConnectionStateLatch(numConnectionEvents);

      objectsLatch = new ObjectsLatch(objectsToCreate);
      for (int j = 0; j < numPipelinesToCreate; j++) {
        MediaPipeline mp = kurentoClient.createMediaPipeline();
        allMediaPipelines.add(mp);

        ArrayList<PassThrough> eachPassThroughChildren = new ArrayList<PassThrough>();

        // Create passthroughs
        for (int k = 0; k < passthroughToCreate; k++) {
          PassThrough passThrough = new PassThrough.Builder(mp).build();
          passThrough.addElementConnectedListener(new EventListener<ElementConnectedEvent>() {

            @Override
            public void onEvent(ElementConnectedEvent event) {
              connectionStateLatch.getStateConnectedLatch().countDown();
            }
          });

          passThrough.addElementDisconnectedListener(new EventListener<ElementDisconnectedEvent>() {

            @Override
            public void onEvent(ElementDisconnectedEvent event) {
              connectionStateLatch.getStateDisconnectedLatch().countDown();
            }
          });
          allPassThroughs.add(passThrough);
          eachPassThroughChildren.add(passThrough);
        }

        // Connect
        if (eachPassThroughChildren.size() > 0) {
          for (int k = 1; k < eachPassThroughChildren.size(); k++) {
            PassThrough prev = eachPassThroughChildren.get(k - 1);
            PassThrough current = eachPassThroughChildren.get(k);
            prev.connect(current);
          }
        }
        mediaPipelineChildren.put(mp, eachPassThroughChildren);
      }

      // Wait to all objects are created
      Assert.assertTrue(
          "The Objects are not created properly. Expected: " + objectsToCreate + ". No received "
              + (objectsToCreate - objectsLatch.getObjectsCreatedLatch().getCount())
              + " ObjectCreated event(s)",
          objectsLatch.getObjectsCreatedLatch().await(TIMEOUT, TimeUnit.SECONDS));

      // Wait to all object are connected
      Assert.assertTrue(
          "The Objects are not connected properly. Expected: " + numConnectionEvents
              + ". No received "
              + (numConnectionEvents - connectionStateLatch.getStateConnectedLatch().getCount())
              + " Objects connected event(s)",
          connectionStateLatch.getStateConnectedLatch().await(TIMEOUT, TimeUnit.SECONDS));

      // Disconnect
      for (Entry<MediaPipeline, ArrayList<PassThrough>> element : mediaPipelineChildren
          .entrySet()) {
        ArrayList<PassThrough> eachPassThroughChildren = element.getValue();
        if (eachPassThroughChildren.size() > 0) {
          for (int k = 1; k < eachPassThroughChildren.size(); k++) {
            PassThrough prev = eachPassThroughChildren.get(k - 1);
            PassThrough current = eachPassThroughChildren.get(k);
            prev.disconnect(current);
          }
        }
      }

      // Wait to all object are disconnected
      Assert
          .assertTrue(
              "The Objects are not disconnected properly. Expected: " + numConnectionEvents
                  + ". No received "
                  + (numConnectionEvents
                      - connectionStateLatch.getStateDisconnectedLatch().getCount())
                  + " Objects disconnected event(s)",
              connectionStateLatch.getStateDisconnectedLatch().await(TIMEOUT, TimeUnit.SECONDS));

      if (destroyEachPassThrough) {
        // Release all webRtcEndpoints
        for (PassThrough passThrough : allPassThroughs) {
          passThrough.release();
        }
      }

      // Release each MediaPipeline
      for (MediaPipeline pipeline : allMediaPipelines) {
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
