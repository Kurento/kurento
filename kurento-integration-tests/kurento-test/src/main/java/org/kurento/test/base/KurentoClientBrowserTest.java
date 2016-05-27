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

package org.kurento.test.base;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.junit.After;
import org.junit.Before;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebPage;
import org.kurento.test.lifecycle.FailedTest;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.services.FakeKmsService;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;
import org.kurento.test.services.WebServerService;
import org.kurento.test.utils.WebRtcConnector;

/**
 * Base for tests using Kurento client tests with browsers.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class KurentoClientBrowserTest<W extends WebPage> extends BrowserTest<W> {
  @Service
  public static WebServerService webServer = new WebServerService();
  @Service
  public static KmsService kms = new KmsService();
  @Service
  public static FakeKmsService fakeKms = new FakeKmsService();

  protected static KurentoClient kurentoClient;
  protected static KurentoClient fakeKurentoClient;

  @Before
  public void setupKurentoClient() {
    kurentoClient = kms.getKurentoClient();
  }

  @After
  public void teardownKurentoClient() throws Exception {
    kms.closeKurentoClient();
  }

  @FailedTest
  public static void retrieveGstreamerDots() {
    if (kurentoClient != null) {
      try {
        List<MediaPipeline> pipelines = kurentoClient.getServerManager().getPipelines();
        log.debug("Retrieving GStreamerDots for all pipelines in KMS ({})", pipelines.size());

        for (MediaPipeline pipeline : pipelines) {

          String pipelineName = pipeline.getName();
          log.debug("Saving GstreamerDot for pipeline {}", pipelineName);

          String gstreamerDotFile = KurentoTest.getDefaultOutputFile("-" + pipelineName);

          try {
            FileUtils.writeStringToFile(new File(gstreamerDotFile), pipeline.getGstreamerDot());

          } catch (IOException ioe) {
            log.error("Exception writing GstreamerDot file", ioe);
          }
        }
      } catch (WebSocketException e) {
        log.warn("WebSocket exception while reading existing pipelines. Maybe KMS is closed: {}:{}",
            e.getClass().getName(), e.getMessage());
      }
    }
  }

  protected String getDefaultFileForRecording() {
    return getDefaultOutputFile(".webm");
  }

  public void addFakeClients(MediaPipeline mainPipeline, WebRtcEndpoint senderWebRtcEndpoint,
      int numFakeClients, long timeBetweenClientMs) {
    fakeKms.addFakeClients(numFakeClients, -1, mainPipeline, senderWebRtcEndpoint,
        timeBetweenClientMs, null, null);
  }

  public void addFakeClients(int numFakeClients, int bandwidht, MediaPipeline mainPipeline,
      WebRtcEndpoint senderWebRtcEndpoint) {
    fakeKms.addFakeClients(numFakeClients, bandwidht, mainPipeline, senderWebRtcEndpoint, 0, null,
        null);
  }

  public void addFakeClients(int numFakeClients, MediaPipeline mainPipeline,
      WebRtcEndpoint senderWebRtcEndpoint, long timeBetweenClientMs, SystemMonitorManager monitor,
      WebRtcConnector connector) {
    fakeKms.addFakeClients(numFakeClients, -1, mainPipeline, senderWebRtcEndpoint,
        timeBetweenClientMs, monitor, connector);
  }

}
