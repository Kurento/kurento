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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.ElementConnectedEvent;
import org.kurento.client.ElementDisconnectedEvent;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaFlowOutStateChangedEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ObjectCreatedEvent;
import org.kurento.client.ObjectDestroyedEvent;
import org.kurento.client.PassThrough;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Stability test for Pipelines, WebRTCEndpoints and PassThroughs. Connect/Disconnect several
 * Passthroughs to one WebRTC like a chain where the WebRTC is the first element.<br/>
 *
 * <pre>
 * Pipeline -> WebRTCEndpoint -> PassThrough -> PassThrough -> ... -> PassThrough
 * </pre>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br/>
 * <ul>
 * <li>The test procedure will be repeated during i iterations</li>
 * <li>N is 2^i</li>
 * </ul>
 *
 * Test procedure: <br/>
 * <ol>
 * <li>Create M pipelines, it will be a constant value</li></li>
 * <li>Create one webRTCEndPoint root in each pipeline and configure to send audio/video</li>
 * <li>Create N passThroughs in each pipeline and connect to webRTCEndPoint root like a chain where
 * the WebRTC is the first element</li>
 * <li>Disconnect N passThroughs from each webRTCEndPoint root</li>
 * <li>Release all passThroughs and webRTCEndPoints. Depends of the test, this point will be
 * executed or not. See doTest method</li>
 * <li>Release M pipelines</li>
 * </ol>
 *
 * Main assertion(s):
 * <ul>
 * <li>((M * N) + M * 2) Object Created Events are received</li>
 * <li>((M * N) + M * 2) Object Destroyed Events are received</li>
 * <li>(M * N) * 3 Connected Events are received. Each pipeline will have N connections, and each
 * connection launches 3 events (VIDEO, AUDIO and DATA)</li>
 * <li>(M * N) * 3 Disconnected Events are received. Each pipeline will have N disconnections, and
 * each disconnection launches 3 events (VIDEO, AUDIO and DATA)</li>
 * <li>FLOWING OUT Event is received</li>
 * <li>After the memory is stabilized, the % of the memory is between 0 and 10</li>
 * </ul>
 *
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */

@Ignore
public class PipelineStabilityConnectDisconnectWebRtcAndPassthroughChainedTest
    extends BasePipeline {
  private int INCREASE_EXPONENTIAL = 2;
  private int ITERATIONS = 5;
  private static int M = 2;
  private ObjectsLatch objectsLatch;
  private ConnectionStateLatch connectionStateLatch;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromesAndFirefoxs(M);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughChainedOnePipelineDestroyPipeline()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, false, false);
    // Use the assert for checking memory
    doTest(1, false, true);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughChainedOnePipelineDestroyEachElement()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, true, false);
    // Use the assert for checking memory
    doTest(1, true, true);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughChainedMPipelineDestroyPipeline()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(M, false, false);
    // Use the assert for checking memory
    doTest(M, false, true);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughChainedMPipelineDestroyEachElement()
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
   * @param destroyEachElement
   *          Define if each element will be released one by one
   * @param checkMemory
   *          Activate the memory assert.
   * @throws Exception
   */
  private void doTest(int numPipelinesToCreate, boolean destroyEachElement, boolean checkMemory)
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
      HashMap<MediaPipeline, ArrayList<PassThrough>> mediaPipelinePassThroughChildren =
          new HashMap<MediaPipeline, ArrayList<PassThrough>>();

      HashMap<MediaPipeline, WebRtcEndpoint> mediaPipelineWebRtcChildren =
          new HashMap<MediaPipeline, WebRtcEndpoint>();

      passthroughToCreate = (int) Math.pow(INCREASE_EXPONENTIAL, i);
      // In this case numPipelinesToCreate * 2 because for each pipeline, one webRTC is created
      // as root
      objectsToCreate = (passthroughToCreate * numPipelinesToCreate) + numPipelinesToCreate * 2;

      log.debug(
          "Create {} MediaPipelines; Create 1 WebRtcEndpoint by MediaPipeline; Create {} Passthrough by MediaPipeline; Total: {} objects",
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

        WebRtcEndpoint webRtcRoot = new WebRtcEndpoint.Builder(mp).build();
        ArrayList<PassThrough> eachPassThroughChildren = new ArrayList<PassThrough>();
        webRtcRoot.setName("webRtcRoot");

        mediaPipelineWebRtcChildren.put(mp, webRtcRoot);

        final CountDownLatch flowingLatch = new CountDownLatch(1);
        webRtcRoot
            .addMediaFlowOutStateChangedListener(new EventListener<MediaFlowOutStateChangedEvent>() {

              @Override
              public void onEvent(MediaFlowOutStateChangedEvent event) {
                if (event.getState().equals(MediaFlowState.FLOWING)) {
                  flowingLatch.countDown();
                }
              }
            });

        webRtcRoot.addElementConnectedListener(new EventListener<ElementConnectedEvent>() {

          @Override
          public void onEvent(ElementConnectedEvent event) {
            connectionStateLatch.getStateConnectedLatch().countDown();
          }
        });

        webRtcRoot.addElementDisconnectedListener(new EventListener<ElementDisconnectedEvent>() {

          @Override
          public void onEvent(ElementDisconnectedEvent event) {
            connectionStateLatch.getStateDisconnectedLatch().countDown();
          }
        });

        getPage(j).initWebRtc(webRtcRoot, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);

        Assert.assertTrue("Not received FLOWING OUT event in webRtcRoot",
            flowingLatch.await(getPage(j).getTimeout(), TimeUnit.SECONDS));

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

        // Connect like a chain
        if (eachPassThroughChildren.size() > 0) {
          webRtcRoot.connect(eachPassThroughChildren.get(0));
          for (int k = 1; k < eachPassThroughChildren.size(); k++) {
            PassThrough prev = eachPassThroughChildren.get(k - 1);
            PassThrough current = eachPassThroughChildren.get(k);
            prev.connect(current);
          }
        }
        mediaPipelinePassThroughChildren.put(mp, eachPassThroughChildren);
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
      for (Entry<MediaPipeline, ArrayList<PassThrough>> element : mediaPipelinePassThroughChildren
          .entrySet()) {
        ArrayList<PassThrough> eachPassThroughChildren = element.getValue();
        if (eachPassThroughChildren.size() > 0) {
          WebRtcEndpoint webRtcRoot = mediaPipelineWebRtcChildren.get(element.getKey());
          webRtcRoot.disconnect(eachPassThroughChildren.get(0));
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

      if (destroyEachElement) {
        // Release all passThroughs
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
