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
 * Stability test for Pipeline, WebRtcEndpoints and PassThroughs. Connect/Disconnect elements like
 * one to many with a WebRTC like root and it is sending media, but the initialization of the WebRTC
 * is executed while the PassThroughs are connected to WebRTC<br/>
 *
 * <pre>
 *                                  PassThrough
 *                                /
 * Pipeline -> WebRTCEndpointRoot --> ....
 *                                \
 *                                  PassThrough
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
 * <li>txX is the thread</li>
 * </ul>
 *
 * Test procedure: <br/>
 * <ol>
 * <li>(tx1) Create M pipelines, it will be a constant value</li>
 * <li>(tx2) Create one webRTCEndPoint root in each pipeline and configure to send audio/video</li>
 * <li>(tx1) Create N passThroughs in each pipeline and connect to webRTCEndPoint root like one to
 * many</li>
 * <li>(tx1) Disconnect N passThroughs from each webRTCEndPoint root</li>
 * <li>(tx1) Release all passThroughs and webRTCEndPoints. Depends of the test, this point will be
 * executed or not. See doTest method</li>
 * <li>(tx1) Release M pipelines</li>
 * </ol>
 *
 * Main assertion(s):
 * <ul>
 * <li>((M * N) + M * 2) Object_Created are received. Also, each pipeline will have one
 * WebRtcEnpoint, for this reason -> M * 2</li>
 * <li>((M * N) + M * 2) Object_Destroyed are received. Also, each pipeline will have one
 * WebRtcEnpoint, for this reason -> M * 2</li>
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
public class PipelineStabilityConnectDisconnectWebRtcAndPassthroughOneToManyParallelTest
    extends BasePipeline {
  private int INCREASE_EXPONENTIAL = 2;
  private int ITERATIONS = 6;
  private static int M = 2;
  private ObjectsLatch objectsLatch;
  private ConnectionStateLatch connectionStateLatch;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromesAndFirefoxs(M);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughOneToManyParallelOnePipelineDestroyPipeline()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, false, false);
    // Use the assert for checking memory
    doTest(1, false, true);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughOneToManyParallelOnePipelineDestroyEachElement()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(1, true, false);
    // Use the assert for checking memory
    doTest(1, true, true);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughOneToManyParallelMPipelineDestroyPipeline()
      throws Exception {
    // Stabilize the memory on the KMS
    doTest(M, false, false);
    // Use the assert for checking memory
    doTest(M, false, true);
  }

  @Ignore
  public void testConnectDisconnectWebRtcAndPassthroughOneToManyParallelMPipelineDestroyEachElement()
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
   *          Define if each element will be released one by one before releasing the pipeline
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
      HashMap<WebRtcEndpoint, ArrayList<PassThrough>> webRtcRootChildren =
          new HashMap<WebRtcEndpoint, ArrayList<PassThrough>>();

      passthroughToCreate = (int) Math.pow(INCREASE_EXPONENTIAL, i);
      // In this case numPipelinesToCreate * 2 because for each pipeline, one webRTC is created
      // as root
      objectsToCreate = (passthroughToCreate * numPipelinesToCreate) + numPipelinesToCreate * 2;

      log.debug(
          "Create {} MediaPipelines; Create 1 WebRtcEndpoint as root by MediaPipeline; Create {} Passthroughs by MediaPipeline; Total: {} objects",
          numPipelinesToCreate, passthroughToCreate, objectsToCreate);

      ArrayList<PassThrough> allPassThroughs = new ArrayList<PassThrough>();
      ArrayList<MediaPipeline> allMediaPipelines = new ArrayList<MediaPipeline>();

      // Each pipeline will have passthroughToCreate connections, and each connection launches 3
      // events (VIDEO, AUDIO and DATA)
      int numConnectionEvents = (passthroughToCreate * numPipelinesToCreate) * 3;
      connectionStateLatch = new ConnectionStateLatch(numConnectionEvents);

      objectsLatch = new ObjectsLatch(objectsToCreate);
      for (int j = 0; j < numPipelinesToCreate; j++) {
        final int browser = j;
        MediaPipeline mp = kurentoClient.createMediaPipeline();
        allMediaPipelines.add(mp);

        final WebRtcEndpoint webRtcRoot = new WebRtcEndpoint.Builder(mp).build();
        ArrayList<PassThrough> eachPassThroughChildren = new ArrayList<PassThrough>();
        webRtcRoot.setName("webRtcRoot");

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

        Thread th2 = new Thread(new Runnable() {

          @Override
          public void run() {
            try {
              getPage(browser).initWebRtc(webRtcRoot, WebRtcChannel.AUDIO_AND_VIDEO,
                  WebRtcMode.SEND_RCV);

            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        });
        th2.start();

        for (int k = 0; k < passthroughToCreate; k++) {
          PassThrough passThrough = new PassThrough.Builder(mp).build();
          allPassThroughs.add(passThrough);
          eachPassThroughChildren.add(passThrough);
          webRtcRoot.connect(passThrough);
        }
        webRtcRootChildren.put(webRtcRoot, eachPassThroughChildren);

        Assert.assertTrue("Not received FLOWING OUT event in webRtcRoot",
            flowingLatch.await(getPage(browser).getTimeout(), TimeUnit.SECONDS));
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
      for (Entry<WebRtcEndpoint, ArrayList<PassThrough>> element : webRtcRootChildren.entrySet()) {
        WebRtcEndpoint webRtcRoot = element.getKey();
        webRtcRoot.addElementDisconnectedListener(new EventListener<ElementDisconnectedEvent>() {

          @Override
          public void onEvent(ElementDisconnectedEvent event) {
            connectionStateLatch.getStateDisconnectedLatch().countDown();
          }
        });
        for (PassThrough passThrough : element.getValue()) {
          webRtcRoot.disconnect(passThrough);
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
        // Release all webRTCs
        for (Entry<WebRtcEndpoint, ArrayList<PassThrough>> element : webRtcRootChildren
            .entrySet()) {
          WebRtcEndpoint webRtcRoot = element.getKey();
          webRtcRoot.release();
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
