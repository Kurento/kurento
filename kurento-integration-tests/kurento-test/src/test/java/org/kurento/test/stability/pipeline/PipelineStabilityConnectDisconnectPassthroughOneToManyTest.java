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
 * Stability test for Pipelines and PassThrough. Connect/Disconnect elements like one to many where
 * there is a one PassThrough like root <br/>
 *
 * <pre>
 *                                  PassThrough
 *                                /
 * Pipeline -> PassThroughRoot --> ....
 *                                \
 *                                  PassThrough
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
 * <li>Create one passThrough root in each pipeline</li>
 * <li>Create N passThrough in each pipeline and connect to passThrough root like one to many</li>
 * <li>Disconnect N passThrough from each pasThrough root</li>
 * <li>Release all passThrough. Depends of the test, this point will be executed or not</li>
 * <li>Release M pipelines</li>
 * </ol>
 *
 * Main assertion(s):
 * <ul>
 * <li>((M * N) + M * 2) Object_Created are received</li>
 * <li>((M * N) + M * 2) Object_Destroyed are received</li>
 * <li>(M * N) * 3 Connected Events are received. Each pipeline will have N connections, and each
 * connection launches 3 events (VIDEO, AUDIO and DATA)</li>
 * <li>(M * N) * 3 Disconnected Events are received. Each pipeline will have N connections, and each
 * connection launches 3 events (VIDEO, AUDIO and DATA)</li>
 * <li>The % of the memory is between 0 and 10</li>
 * </ul>
 *
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */

@Ignore
public class PipelineStabilityConnectDisconnectPassthroughOneToManyTest extends BasePipeline {
  private int INCREASE_EXPONENTIAL = 2;
  private int ITERATIONS = 9;
  private int M = 4;
  private ObjectsLatch objectsLatch;
  private ConnectionStateLatch connectionStateLatch;

  @Ignore
  public void testConnectDisconnectPassthroughOneToManyOnePipelineDestroyPipeline()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, false, false);
    // Use the assert for checking memory
    doTest(1, false, true);
  }

  @Ignore
  public void testConnectDisconnectPassthroughOneToManyOnePipelineDestroyEachPassThrough()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, true, false);
    // Use the assert for checking memory
    doTest(1, true, true);
  }

  @Ignore
  public void testConnectDisconnectPassthroughOneToManyMPipelineDestroyPipeline() throws Exception {
    // Stabilize the memory on the KMS
    doTest(M, false, false);
    // Use the assert for checking memory
    doTest(M, false, true);
  }

  @Ignore
  public void testConnectDisconnectPassthroughOneToManyMPipelineDestroyEachPassThrough()
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
      HashMap<PassThrough, ArrayList<PassThrough>> passThroughRootChildren =
          new HashMap<PassThrough, ArrayList<PassThrough>>();

      passthroughToCreate = (int) Math.pow(INCREASE_EXPONENTIAL, i);
      // In this case numPipelinesToCreate * 2 because for each pipeline, one passThrough is created
      // as root
      objectsToCreate = (passthroughToCreate * numPipelinesToCreate) + numPipelinesToCreate * 2;

      log.debug(
          "Create {} MediaPipelines; Create 1 Passthrough as root by MediaPipeline; Create {} Passthrough by MediaPipeline; Total: {} objects",
          numPipelinesToCreate, passthroughToCreate, objectsToCreate);

      ArrayList<PassThrough> allPassThroughs = new ArrayList<PassThrough>();
      ArrayList<MediaPipeline> allMediaPipelines = new ArrayList<MediaPipeline>();

      // Each pipeline will have passthroughToCreate connections, and each connection launches 3
      // events (VIDEO, AUDIO and DATA)
      int numConnectionEvents = (passthroughToCreate * numPipelinesToCreate) * 3;
      connectionStateLatch = new ConnectionStateLatch(numConnectionEvents);

      objectsLatch = new ObjectsLatch(objectsToCreate);
      for (int j = 0; j < numPipelinesToCreate; j++) {
        MediaPipeline mp = kurentoClient.createMediaPipeline();
        allMediaPipelines.add(mp);

        PassThrough passThroughRoot = new PassThrough.Builder(mp).build();
        ArrayList<PassThrough> eachPassThroughChildren = new ArrayList<PassThrough>();
        passThroughRoot.setName("passThroughRoot");
        allPassThroughs.add(passThroughRoot);

        passThroughRoot.addElementConnectedListener(new EventListener<ElementConnectedEvent>() {

          @Override
          public void onEvent(ElementConnectedEvent event) {
            connectionStateLatch.getStateConnectedLatch().countDown();
          }
        });

        passThroughRoot
            .addElementDisconnectedListener(new EventListener<ElementDisconnectedEvent>() {

              @Override
              public void onEvent(ElementDisconnectedEvent event) {
                connectionStateLatch.getStateDisconnectedLatch().countDown();
              }
            });

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
          passThroughRoot.connect(passThrough);
        }
        passThroughRootChildren.put(passThroughRoot, eachPassThroughChildren);
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

      // Disconnect elements
      for (Entry<PassThrough, ArrayList<PassThrough>> element : passThroughRootChildren
          .entrySet()) {
        PassThrough passThroughRoot = element.getKey();
        for (PassThrough passThrough : element.getValue()) {
          passThroughRoot.disconnect(passThrough);
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
