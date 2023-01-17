/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.performance.kms;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.Filter;
import org.kurento.client.FilterType;
import org.kurento.client.GStreamerFilter;
import org.kurento.client.ImageOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.ZBarFilter;
import org.kurento.module.chroma.ChromaFilter;
import org.kurento.module.chroma.WindowParam;
import org.kurento.module.crowddetector.CrowdDetectorFilter;
import org.kurento.module.crowddetector.RegionOfInterest;
import org.kurento.module.crowddetector.RegionOfInterestConfig;
import org.kurento.module.crowddetector.RelativePoint;
import org.kurento.module.platedetector.PlateDetectorFilter;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.utils.WebRtcConnector;

/**
 * <strong>Description</strong>: Performance test for KMS.<br/>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No assertion, just data gathering.</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */
public class KmsPerformanceTest extends PerformanceTest {

  private enum MediaProcessingType {
    NONE, ENCODER, FILTER, FACEOVERLAY, IMAGEOVERLAY, ZBAR, PLATEDETECTOR, CHROMA, CROWDDETECTOR
  }

  private static final String MEDIA_PROCESSING_PROPERTY = "mediaProcessing";
  private static final String MEDIA_PROCESSING_DEFAULT =
      MediaProcessingType.NONE.name().toLowerCase();

  private static final String NUM_CLIENTS_PROPERTY = "numClients";
  private static final int NUM_CLIENTS_DEFAULT = 2;

  private static final String TIME_BEETWEEN_CLIENTS_PROPERTY = "timeBetweenClientCreation";
  private static final int TIME_BEETWEEN_CLIENTS_DEFAULT = 0; // milliseconds

  private static final String PERFORMANCE_TEST_TIME_PROPERTY = "performanceTestTime";
  private static final int PERFORMANCE_TEST_TIME_DEFAULT = 10; // seconds

  private static final String OUTPUT_FILE_PROPERTY = "outputFile";
  private static final String OUTPUT_FILE_DEFAULT = "./kms-results.csv";

  private static final String GET_KMS_LATENCY_PROPERTY = "getKmsLatency";
  private static final boolean GET_KMS_LATENCY_DEFAULT = true;

  private static String mediaProcessing =
      getProperty(MEDIA_PROCESSING_PROPERTY, MEDIA_PROCESSING_DEFAULT);
  private static int numClients = getProperty(NUM_CLIENTS_PROPERTY, NUM_CLIENTS_DEFAULT);
  private static int timeBetweenClients =
      getProperty(TIME_BEETWEEN_CLIENTS_PROPERTY, TIME_BEETWEEN_CLIENTS_DEFAULT);
  private static int testTime =
      getProperty(PERFORMANCE_TEST_TIME_PROPERTY, PERFORMANCE_TEST_TIME_DEFAULT);

  private MediaPipeline mp;

  private MediaProcessingType mediaProcessingType;

  public KmsPerformanceTest() {
    setShowLatency(true);
  }

  @Before
  public void setupKmsPerformanceTest() {
    setMonitorResultPath(getProperty(OUTPUT_FILE_PROPERTY, OUTPUT_FILE_DEFAULT));

    try {
      mediaProcessingType = MediaProcessingType.valueOf(mediaProcessing.toUpperCase());
    } catch (IllegalArgumentException e) {
      mediaProcessingType = MediaProcessingType.NONE;
    }
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    int numBrowsers = numClients < 2 ? 2 : 3;
    return TestScenario.localChromesWithRgbVideo(numBrowsers);
  }

  @Test
  public void testKmsPerformance() throws InterruptedException {

    mp = kurentoClient.createMediaPipeline();
    mp.setLatencyStats(getProperty(GET_KMS_LATENCY_PROPERTY, GET_KMS_LATENCY_DEFAULT));

    // 2 latency controllers (1 per real viewer)
    LatencyController[] cs = new LatencyController[2];

    try {
      WebRtcEndpoint inputEndpoint = createInputBrowserClient(getPage(0));
      String firstClientName = "client1";
      WebRtcEndpoint firstWebEndpoint =
          createOutputBrowserClient(firstClientName, getPage(1), inputEndpoint);

      cs[0] = new LatencyController(firstClientName, monitor);
      cs[0].checkLatencyInBackground(getPage(0), getPage(1));

      // Guard time to receive positive values of latency by KMS
      Thread.sleep(10000);

      WebRtcEndpoint lastWebEndpoint = null;
      if (numClients > 1) {
        configureFakeClients(inputEndpoint);
        String lastClientName = "clientN";
        lastWebEndpoint = createOutputBrowserClient(lastClientName, getPage(2), inputEndpoint);

        cs[1] = new LatencyController(lastClientName, monitor);
        cs[1].checkLatencyInBackground(getPage(0), getPage(2));
      }

      // Test time
      waitSeconds(testTime);

      // Remove clients (real and fake)
      inputEndpoint.disconnect(lastWebEndpoint);
      getPage(2).close();
      monitor.decrementNumClients();
      waitMilliSeconds(timeBetweenClients);

      fakeKms.removeAllFakeClients(timeBetweenClients, inputEndpoint, monitor);

      inputEndpoint.disconnect(firstWebEndpoint);
      getPage(1).close();
      monitor.decrementNumClients();
      waitMilliSeconds(timeBetweenClients);

    } finally {
      if (mp != null) {
        mp.release();
      }
    }
  }

  private void connectWithMediaProcessing(WebRtcEndpoint inputEndpoint,
      WebRtcEndpoint outputEndpoint) {

    switch (mediaProcessingType) {
      case ENCODER:
        Filter filter = new GStreamerFilter.Builder(mp, "capsfilter caps=video/x-raw")
            .withFilterType(FilterType.VIDEO).build();
        inputEndpoint.connect(filter);
        filter.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> GStreamerFilter -> WebRtcEndpoint");
        break;

      case FILTER:
      case FACEOVERLAY:
        filter = new FaceOverlayFilter.Builder(mp).build();
        inputEndpoint.connect(filter);
        filter.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> FaceOverlayFilter -> WebRtcEndpoint");
        break;

      case ZBAR:
        filter = new ZBarFilter.Builder(mp).build();
        inputEndpoint.connect(filter);
        filter.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> ZBarFilter -> WebRtcEndpoint");
        break;

      case IMAGEOVERLAY:
        filter = new ImageOverlayFilter.Builder(mp).build();
        inputEndpoint.connect(filter);
        filter.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> ImageOverlayFilter -> WebRtcEndpoint");
        break;

      case PLATEDETECTOR:
        filter = new PlateDetectorFilter.Builder(mp).build();
        inputEndpoint.connect(filter);
        filter.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> PlateDetectorFilter -> WebRtcEndpoint");
        break;

      case CROWDDETECTOR:
        List<RegionOfInterest> rois = getDummyRois();
        filter = new CrowdDetectorFilter.Builder(mp, rois).build();
        inputEndpoint.connect(filter);
        filter.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> CrowdDetectorFilter -> WebRtcEndpoint");
        break;

      case CHROMA:
        filter = new ChromaFilter.Builder(mp, new WindowParam(0, 0, 640, 480)).build();
        inputEndpoint.connect(filter);
        filter.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> ChromaFilter -> WebRtcEndpoint");
        break;

      case NONE:
      default:
        inputEndpoint.connect(outputEndpoint);
        log.debug("Pipeline: WebRtcEndpoint -> WebRtcEndpoint");
        break;
    }
  }

  private void configureFakeClients(WebRtcEndpoint inputWebRtc) {

    int numFakeClients = numClients - 2;

    if (numFakeClients > 0) {
      log.debug("Adding {} fake clients", numFakeClients);
      addFakeClients(numFakeClients, mp, inputWebRtc, timeBetweenClients, monitor,
          new WebRtcConnector() {
            @Override
            public void connect(WebRtcEndpoint inputEndpoint, WebRtcEndpoint outputEndpoint) {
              connectWithMediaProcessing(inputEndpoint, outputEndpoint);
            }
          });
    }
  }

  private WebRtcEndpoint createInputBrowserClient(WebRtcTestPage page) throws InterruptedException {

    WebRtcEndpoint inputEndpoint = new WebRtcEndpoint.Builder(mp).build();

    page.initWebRtc(inputEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

    monitor.addWebRtcClientAndActivateOutboundStats("sender", inputEndpoint, page,
        "webRtcPeer.peerConnection");

    return inputEndpoint;
  }

  private WebRtcEndpoint createOutputBrowserClient(String id, WebRtcTestPage page,
      WebRtcEndpoint inputWebRtc) throws InterruptedException {

    WebRtcEndpoint outputEndpoint = new WebRtcEndpoint.Builder(mp).build();

    connectWithMediaProcessing(inputWebRtc, outputEndpoint);

    page.initWebRtc(outputEndpoint, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    monitor.incrementNumClients();

    monitor.addWebRtcClientAndActivateInboundStats(id, outputEndpoint, page,
        "webRtcPeer.peerConnection");

    return outputEndpoint;
  }

  private List<RegionOfInterest> getDummyRois() {

    List<RelativePoint> points = new ArrayList<>();

    float x = 0;
    float y = 0;
    points.add(new RelativePoint(x, y));

    x = 1;
    y = 0;
    points.add(new RelativePoint(x, y));

    x = 1;
    y = 1;
    points.add(new RelativePoint(x, y));

    x = 0;
    y = 1;
    points.add(new RelativePoint(x, y));

    RegionOfInterestConfig config = new RegionOfInterestConfig();

    config.setFluidityLevelMin(10);
    config.setFluidityLevelMed(35);
    config.setFluidityLevelMax(65);
    config.setFluidityNumFramesToEvent(5);
    config.setOccupancyLevelMin(10);
    config.setOccupancyLevelMed(35);
    config.setOccupancyLevelMax(65);
    config.setOccupancyNumFramesToEvent(5);

    config.setSendOpticalFlowEvent(false);

    config.setOpticalFlowNumFramesToEvent(3);
    config.setOpticalFlowNumFramesToReset(3);
    config.setOpticalFlowAngleOffset(0);

    List<RegionOfInterest> rois = new ArrayList<>();
    rois.add(new RegionOfInterest(points, config, "dummyRoy"));

    return rois;
  }
}
