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

package org.kurento.test.functional.webrtc;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.monitor.SystemMonitorManager;

/**
 * WebRTC one to many test.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> N X WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint presenter connected to N viewers</li>
 * <li>(Browser) 1 WebRtcPeer in send-only sends media. N WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag of the viewers</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcFourOneToManyTest extends FunctionalTest {

  private static final int PLAYTIME = 40; // seconds
  private static final int DEFAULT_NUM_VIEWERS = 3;
  private static int numViewers;
  private SystemMonitorManager monitor;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    numViewers = getProperty("webrtc.one2many.numviewers", DEFAULT_NUM_VIEWERS);

    // Test: 1 presenter + N viewers (all local Chrome's)
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.PRESENTER,
        new Browser.Builder().webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/15sec/rgbHD.y4m")
            .build());
    test.addBrowser(BrowserConfig.VIEWER,
        new Browser.Builder().webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
            .scope(BrowserScope.LOCAL).numInstances(numViewers).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  // TODO: Commented due to Hijack issue on Docker
  // @Before
  // public void setupMonitor() {
  // setDeleteLogsIfSuccess(false);
  // monitor = new SystemMonitorManager();
  // monitor.setShowLantency(true);
  // monitor.startMonitoring();
  // }
  //
  // @After
  // public void teardownMonitor() throws IOException {
  // if (monitor != null) {
  // monitor.stop();
  // monitor.writeResults(getDefaultOutputFile("-monitor.csv"));
  // monitor.destroy();
  // }
  // }

  @Test
  public void testWebRtcOneToManyChrome() throws InterruptedException, IOException {
    // Media Pipeline
    final MediaPipeline mp = kurentoClient.createMediaPipeline();
    final WebRtcEndpoint masterWebRtcEp = new WebRtcEndpoint.Builder(mp).build();

    // Assets for viewers
    final LatencyController[] cs = new LatencyController[numViewers];
    final WebRtcEndpoint[] viewerWebRtcEPs = new WebRtcEndpoint[numViewers];
    final CountDownLatch latch = new CountDownLatch(numViewers);

    // Presenter
    getPresenter().subscribeLocalEvents("playing");
    getPresenter().initWebRtc(masterWebRtcEp, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_ONLY);

    if (monitor != null) {
      monitor.addWebRtcClientAndActivateOutboundStats(getPresenter().getBrowser().getId(),
          masterWebRtcEp, getPresenter(), "webRtcPeer.peerConnection");
    }

    // Viewers
    ExecutorService exec = Executors.newFixedThreadPool(numViewers);
    for (int j = 0; j < numViewers; j++) {
      final int i = j;
      Thread thread = new Thread() {
        @Override
        public void run() {
          try {
            viewerWebRtcEPs[i] = new WebRtcEndpoint.Builder(mp).build();
            masterWebRtcEp.connect(viewerWebRtcEPs[i]);
            if (monitor != null) {
              monitor.incrementNumClients();
            }

            // Latency control
            String name = getViewer(i).getBrowser().getId();
            cs[i] = new LatencyController(name, monitor);

            // WebRTC
            getViewer(i).subscribeEvents("playing");
            getViewer(i).initWebRtc(viewerWebRtcEPs[i], WebRtcChannel.VIDEO_ONLY,
                WebRtcMode.RCV_ONLY);
            if (monitor != null) {
              monitor.addWebRtcClientAndActivateInboundStats(getViewer(i).getBrowser().getId(),
                  viewerWebRtcEPs[i], getViewer(i), "webRtcPeer.peerConnection");
            }

            // Latency assessment
            cs[i].checkLatency(PLAYTIME, TimeUnit.SECONDS, getPresenter(), getViewer(i));
            cs[i].drawChart(getDefaultOutputFile("-" + name + "-latency.png"), 500, 270);
            cs[i].writeCsv(getDefaultOutputFile("-" + name + "-latency.csv"));
            cs[i].logLatencyErrorrs();
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            latch.countDown();
            if (monitor != null) {
              monitor.decrementNumClients();
            }
          }
        }
      };
      exec.execute(thread);
    }

    // Wait to finish viewers threads
    latch.await();

    // Release Media Pipeline
    mp.release();
  }
}
