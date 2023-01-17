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

package org.kurento.test.scalability.webrtc;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.base.ScalabilityTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.ChartWriter;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.LatencyRegistry;
import org.kurento.test.latency.VideoTagType;

/**
 * <strong>Description</strong>: Stability test for WebRTC in loopback with N fake clients.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * <li>WebRtcEndpoint -> N Fake WebRtcEndpoint's</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Color change should be detected on local/remote video tag of browsers</li>
 * <li>Test fail when 3 consecutive latency errors (latency > 3sec) are detected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */

public class WebRtcScalabilityLatencyTest extends ScalabilityTest {

  private static final int WIDTH = 500;
  private static final int HEIGHT = 270;

  private static int playTime = getProperty("test.scalability.latency.playtime", 30); // seconds
  private static int bandWidth = getProperty("test.scalability.latency.bandwidth", 500);
  private static int realClients = getProperty("test.scalability.latency.realclients", 1);
  private static String[] fakeClientsArray =
      getProperty("test.scalability.latency.fakeclients", "0,20,40,60,80,100").split(",");

  private static Map<Long, LatencyRegistry> latencyResult = new HashMap<>();

  private int fakeClients;

  public WebRtcScalabilityLatencyTest(int fakeClients) {
    this.fakeClients = fakeClients;
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    String videoPath = KurentoTest.getTestFilesDiskPath() + "/video/15sec/rgbHD.y4m";
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.BROWSER, new Browser.Builder().webPageType(WebPageType.WEBRTC)
        .browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).video(videoPath).build());

    for (int i = 0; i < realClients; i++) {
      test.addBrowser(BrowserConfig.BROWSER + i,
          new Browser.Builder().webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
              .scope(BrowserScope.LOCAL).build());
    }

    Collection<Object[]> out = new ArrayList<>();
    for (String s : fakeClientsArray) {
      out.add(new Object[] { test, Integer.parseInt(s) });
    }

    return out;
  }

  @Test
  public void testWebRtcScalabilityLatency() throws Exception {

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    // Fake clients
    addFakeClients(fakeClients, bandWidth, mp, webRtcEndpoint);

    // Latency control
    LatencyController cs = new LatencyController("Latency in loopback");

    // WebRTC
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);

    // Real clients
    ExecutorService executor = Executors.newFixedThreadPool(realClients);
    final LatencyController[] csB2B = new LatencyController[realClients];
    for (int i = 0; i < realClients; i++) {
      csB2B[i] = new LatencyController("Latency in back2back " + i);
      WebRtcEndpoint extraWebRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
      webRtcEndpoint.connect(extraWebRtcEndpoint);

      getPage(i).subscribeEvents("playing");
      getPage(i).initWebRtc(extraWebRtcEndpoint, WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);

      final int j = i;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          csB2B[j].checkLatency(playTime, TimeUnit.SECONDS, getPage(), getPage(j));
        }
      });
    }

    // Latency assessment
    getPage().activateLatencyControl(VideoTagType.LOCAL.getId(), VideoTagType.REMOTE.getId());
    cs.checkLatency(playTime, TimeUnit.SECONDS, getPage());

    // Release Media Pipeline
    mp.release();

    // Latency results in loopback (PNG chart and CSV file)
    cs.drawChart(getDefaultOutputFile("-loopback-fakeClients" + fakeClients + ".png"), WIDTH,
        HEIGHT);
    cs.writeCsv(getDefaultOutputFile("-loopback-fakeClients" + fakeClients + ".csv"));
    cs.logLatencyErrorrs();

    // Latency average
    Map<Long, LatencyRegistry> latencyMap = cs.getLatencyMap();
    long avgLatency = 0;
    int total = latencyMap.size();
    for (LatencyRegistry lr : latencyMap.values()) {
      avgLatency += lr.getLatency();
    }

    for (int i = 0; i < realClients; i++) {
      csB2B[i].drawChart(
          getDefaultOutputFile("-back2back" + i + "-fakeClients" + fakeClients + ".png"), WIDTH,
          HEIGHT);
      csB2B[i]
          .writeCsv(getDefaultOutputFile("-back2back" + i + "-fakeClients" + fakeClients + ".csv"));
      csB2B[i].logLatencyErrorrs();

      Map<Long, LatencyRegistry> remoteLatencyMap = csB2B[i].getLatencyMap();
      for (LatencyRegistry lr : remoteLatencyMap.values()) {
        avgLatency += lr.getLatency();
      }
      total += remoteLatencyMap.size();
    }

    avgLatency /= total;
    latencyResult.put((long) fakeClients, new LatencyRegistry(avgLatency));
  }

  @AfterClass
  public static void teardown() throws IOException {
    // Write csv
    PrintWriter pw = new PrintWriter(new FileWriter(getDefaultOutputFile("-latency.csv")));
    for (long time : latencyResult.keySet()) {
      pw.println(time + "," + latencyResult.get(time).getLatency());
    }
    pw.close();

    // Draw chart
    ChartWriter chartWriter = new ChartWriter(latencyResult, "Latency avg",
        "Latency of fake clients: " + Arrays.toString(fakeClientsArray), "Number of client(s)",
        "Latency (ms)");
    chartWriter.drawChart(getDefaultOutputFile("-latency-evolution.png"), WIDTH, HEIGHT);
  }

}
