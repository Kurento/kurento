/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.test.benchmark;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.ElementStats;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaLatencyStat;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaType;
import org.kurento.client.PassThrough;
import org.kurento.client.Stats;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.test.base.KurentoClientBrowserTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.monitor.SystemMonitorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Benchmark test aimed to characterize the agnostic component in KMS.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 6.5.1
 */
@Category(SystemFunctionalTests.class)
public class AgnosticBenchmarkTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  private final Logger log = LoggerFactory.getLogger(AgnosticBenchmarkTest.class);

  private static final String PASSTHROUGH_NUMBER_PROP = "passtrough.number";
  private static final int PASSTHROUGH_NUMBER_DEFAULT = 2;
  private static final String SESSION_TIME_PROP = "sesion.time";
  private static final int SESSION_TIME_DEFAULT = 30; // seconds
  private static final String SAMPLING_RATE_PROP = "sampling.rate";
  private static final int SAMPLING_RATE_DEFAULT = 100; // milliseconds
  private static final String OUTPUT_FOLDER_PROP = "output.folder";
  private static final String OUTPUT_FOLDER_DEFAULT = ".";
  private static final String MONITOR_KMS_PROP = "monitor.kms";
  private static final boolean MONITOR_KMS_DEFAULT = false;

  private int passTroughNumber = getProperty(PASSTHROUGH_NUMBER_PROP, PASSTHROUGH_NUMBER_DEFAULT);
  private int sessionTime = getProperty(SESSION_TIME_PROP, SESSION_TIME_DEFAULT);
  private int samplingRate = getProperty(SAMPLING_RATE_PROP, SAMPLING_RATE_DEFAULT);
  private String outputFolder = getProperty(OUTPUT_FOLDER_PROP, OUTPUT_FOLDER_DEFAULT);
  private boolean monitorKms = getProperty(MONITOR_KMS_PROP, MONITOR_KMS_DEFAULT);

  private List<MediaElement> passTroughList = new ArrayList<>(passTroughNumber);
  private SystemMonitorManager monitor;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.PRESENTER, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    test.addBrowser(BrowserConfig.VIEWER, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Before
  public void pre() {
    if (!outputFolder.endsWith(File.separator)) {
      outputFolder += File.separator;
    }
  }

  @Test
  public void test() throws Exception {
    // Media Pipeline
    log.info("Media Pipeline [WebRtcEndpoint -> {} X PassThrough -> WebRtcEndpoint]",
        passTroughNumber);
    MediaPipeline mediaPipeline = kurentoClient.createMediaPipeline();
    mediaPipeline.setLatencyStats(true);

    WebRtcEndpoint presenterWebRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
    presenterWebRtcEndpoint.setName("presenterWebRtcEndpoint");
    WebRtcEndpoint viewerWebRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
    viewerWebRtcEndpoint.setName("viewerWebRtcEndpoint");

    for (int i = 0; i < passTroughNumber; i++) {
      MediaElement passThrough = new PassThrough.Builder(mediaPipeline).build();
      passThrough.setName("passThrough" + i);
      if (i == 0) {
        presenterWebRtcEndpoint.connect(passThrough);
      } else {
        passTroughList.get(i - 1).connect(passThrough);
      }
      passTroughList.add(passThrough);
    }
    passTroughList.get(passTroughList.size() - 1).connect(viewerWebRtcEndpoint);

    // Start WebRTC session
    getPresenter().subscribeLocalEvents("playing");
    getPresenter().initWebRtc(presenterWebRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);
    getPresenter().waitForEvent("playing");

    getViewer().subscribeEvents("playing");
    getViewer().initWebRtc(viewerWebRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.RCV_ONLY);
    getViewer().waitForEvent("playing");

    // KMS Monitor (CPU, memory, etc)
    if (monitorKms) {
      monitor = new SystemMonitorManager();
      monitor.setSamplingTime(samplingRate);
      monitor.startMonitoring();
    }

    // Map for gathering latencies
    final Multimap<String, Object> latencies =
        Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, Object>create());

    // Map for gathering stats time
    final Multimap<String, Object> statTimeMap =
        Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, Object>create());

    final ExecutorService executor = Executors.newFixedThreadPool(passTroughList.size());
    Thread latencyThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          for (int i = 0; i < passTroughList.size(); i++) {
            final MediaElement passTrough = passTroughList.get(i);
            final String passTroughName = "passThrough" + i;
            final String nextMediaElementName =
                (i != passTroughList.size() - 1) ? "passThrough" + (i + 1) : "viewerWebRtcEndpoint";
            executor.execute(new Runnable() {
              @Override
              public void run() {
                try {
                  double l1 = getInputLatency(passTrough, statTimeMap, passTroughName);
                  MediaElement next = passTrough.getSinkConnections().iterator().next().getSink();
                  double l2 = getInputLatency(next, statTimeMap, nextMediaElementName);
                  double latency = (l2 - l1) / 1000; // nanoseconds to microseconds
                  latencies.put(passTroughName + "MicroSec", latency);
                  log.debug("{} latency {} ns", passTroughName, latency);
                } catch (Exception e) {
                  log.warn("Finishing due to {}", e.getMessage());
                }
              }
            });
          }
          waitMilliSeconds(samplingRate);
        }
      }
    });
    latencyThread.start();

    // Wait session time
    waitSeconds(sessionTime);

    // Release latency thread/executor
    executor.shutdownNow();
    latencyThread.interrupt();

    // Stop monitor
    String csvPreffix = outputFolder + this.getClass().getSimpleName() + "-" + passTroughNumber
        + "passTrough" + "-" + sessionTime + "seconds";
    if (monitorKms) {
      monitor.stop();
      monitor.writeResults(csvPreffix + "-monitor.csv");
      monitor.destroy();
    }

    // Release media pipeline
    mediaPipeline.release();

    writeCSV(csvPreffix + "-latency.csv", latencies, true);
    writeCSV(csvPreffix + "-time.csv", statTimeMap, true);

  }

  private double getInputLatency(MediaElement mediaElement, Multimap<String, Object> statTimeMap,
      String mediaElementName) {
    long now = System.currentTimeMillis();
    Map<String, Stats> filterStats = mediaElement.getStats(MediaType.VIDEO);
    long time = System.currentTimeMillis() - now;
    statTimeMap.put(mediaElementName + "MiliSec", time);

    double inputLatency = 0;
    for (Stats s : filterStats.values()) {
      if (s instanceof ElementStats) {
        List<MediaLatencyStat> inputLatencyList = ((ElementStats) s).getInputLatency();
        if (!inputLatencyList.isEmpty()) {
          inputLatency = inputLatencyList.get(0).getAvg();
        }
      }
    }
    return inputLatency;
  }

}
