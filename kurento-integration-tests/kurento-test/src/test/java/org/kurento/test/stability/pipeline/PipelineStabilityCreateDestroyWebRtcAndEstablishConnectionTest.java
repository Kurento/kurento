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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.IceComponentState;
import org.kurento.client.IceComponentStateChangedEvent;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ObjectCreatedEvent;
import org.kurento.client.ObjectDestroyedEvent;
import org.kurento.client.WebRtcEndpoint;

/**
 * Stability test for Pipelines and WebRtcEndpoints. Connect by pairs and establish the connection
 * <br/>
 *
 *
 * <pre>
 * Pipeline:
 * WebRtc1 <-> WebRtc2  WebRtc3 <-> WebRtc4 ..... WebRtcN-1 <-> WebRtcN
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
 * <li>Create M pipelines</li>
 * <li>Create N webRtcEndpoints in each pipeline</li>
 * <li>Connect by pairs and establish the connection</li>
 * <li>Release N webRtcEndpoints. Depends of the test, this point will be executed or not</li>
 * <li>Release M pipelines</li>
 * </ol>
 *
 * Main assertion(s):
 * <ul>
 * <li>((M * N) + M) Object_Created are received</li>
 * <li>((M * N) + M) Object_Destroyed are received</li>
 * <li>N CONNECTED are received</li>
 * <li>The % of the memory is between 0 and 10.
 * </ul>
 *
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */

@Ignore
public class PipelineStabilityCreateDestroyWebRtcAndEstablishConnectionTest extends BasePipeline {
  private int INCREASE_EXPONENTIAL = 2;
  private int ITERATIONS = 11;
  private int M = 4;
  private ObjectsLatch objectsLatch;

  @Ignore
  public void testCreateDestroyWebRtcAndEstablishConnectionOnePipelineDestroyPipeline()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, false, false);
    // Use the assert for checking memory
    doTest(1, false, true);
  }

  @Ignore
  public void testCreateDestroyWebRtcAndEstablishConnectionOnePipelineDestroyEachWebRtc()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, true, false);
    // Use the assert for checking memory
    doTest(1, true, true);
  }

  @Ignore
  public void testCreateDestroyWebRtcAndEstablishConnectionWebRtcMPipelineDestroyPipeline()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(M, false, false);
    // Use the assert for checking memory
    doTest(M, false, true);
  }

  @Ignore
  public void testCreateDestroyWebRtcAndEstablishConnectionMPipelineDestroyEachWebRtc()
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
   * @param destroyEachWebRtc
   *          Define if each webRtc will be released one by one
   * @param checkMemory
   *          Activate the memory assert.
   * @throws Exception
   */
  private void doTest(int numPipelinesToCreate, boolean destroyEachWebRtc, boolean checkMemory)
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

    int webRtcEndpointToCreate = 0;
    int objectsToCreate = 0;

    for (int i = 1; i <= ITERATIONS; i++) {
      webRtcEndpointToCreate = (int) Math.pow(INCREASE_EXPONENTIAL, i);
      objectsToCreate = (webRtcEndpointToCreate * numPipelinesToCreate) + numPipelinesToCreate;

      log.debug(
          "Create {} MediaPipelines; Create {} WebRtcEndpoints by MediaPipeline; Total: {} objects",
          numPipelinesToCreate, webRtcEndpointToCreate, objectsToCreate);

      ArrayList<WebRtcEndpoint> webRtcEndpoints = new ArrayList<WebRtcEndpoint>();
      ArrayList<MediaPipeline> mediaPipelines = new ArrayList<MediaPipeline>();

      final Set<WebRtcEndpoint> webRtcEndpointsConnected = new HashSet<WebRtcEndpoint>();

      objectsLatch = new ObjectsLatch(objectsToCreate);
      final CountDownLatch connectedLatch = new CountDownLatch(webRtcEndpointToCreate);

      for (int j = 0; j < numPipelinesToCreate; j++) {
        MediaPipeline mp = kurentoClient.createMediaPipeline();
        mediaPipelines.add(mp);

        for (int k = 0; k < webRtcEndpointToCreate; k++) {
          final WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
          webRtcEndpoints.add(webRtcEp);

          webRtcEp.addIceComponentStateChangedListener(
              new EventListener<IceComponentStateChangedEvent>() {
                private boolean executed = false;

                @Override
                public void onEvent(IceComponentStateChangedEvent event) {
                  // Only executes once when the state is CONNECTED
                  if (!executed) {
                    if (event.getState().equals(IceComponentState.CONNECTED)) {
                      if (!webRtcEndpointsConnected.contains(event.getSource())) {
                        connectedLatch.countDown();
                      }
                      webRtcEndpointsConnected.add((WebRtcEndpoint) event.getSource());
                      executed = true;
                    }
                  }
                }
              });
        }

        for (int k = 0; k < webRtcEndpoints.size(); k = k + 2) {
          final WebRtcEndpoint webRtcEp1 = webRtcEndpoints.get(k);
          final WebRtcEndpoint webRtcEp2 = webRtcEndpoints.get(k + 1);

          webRtcEp1.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
              webRtcEp2.addIceCandidate(event.getCandidate());
            }
          });

          webRtcEp2.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
              webRtcEp1.addIceCandidate(event.getCandidate());
            }
          });

          webRtcEp1.connect(webRtcEp2);
          webRtcEp2.connect(webRtcEp1);

          String sdpOffer = webRtcEp1.generateOffer();
          String sdpAnswer = webRtcEp2.processOffer(sdpOffer);

          webRtcEp2.gatherCandidates();
          webRtcEp1.processAnswer(sdpAnswer);
          webRtcEp1.gatherCandidates();
        }
      }

      // Wait to all objects are created
      Assert.assertTrue(
          "The Objects are not created properly. Expected: " + objectsToCreate + ". No received "
              + (objectsToCreate - objectsLatch.getObjectsCreatedLatch().getCount())
              + " ObjectCreated event(s)",
          objectsLatch.getObjectsCreatedLatch().await(TIMEOUT, TimeUnit.SECONDS));

      // Wait to all connected events are received
      Assert.assertTrue(
          "All connections are wrong. Expected: " + webRtcEndpointToCreate + ". No received "
              + (webRtcEndpointToCreate - connectedLatch.getCount()) + " CONNECTED event(s)",
          connectedLatch.await(TIMEOUT, TimeUnit.SECONDS));

      Assert.assertTrue(
          (webRtcEndpointToCreate - webRtcEndpointsConnected.size())
              + " webRtcEndpoint(s) weren't connected",
          webRtcEndpointsConnected.size() == webRtcEndpointToCreate);

      if (destroyEachWebRtc) {
        // Release all webRtcEndpoints
        for (WebRtcEndpoint webRtcEp : webRtcEndpoints) {
          webRtcEp.release();
        }
      }

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
