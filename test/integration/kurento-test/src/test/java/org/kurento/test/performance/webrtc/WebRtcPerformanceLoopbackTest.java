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

package org.kurento.test.performance.webrtc;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserRunner;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.grid.ParallelBrowsers;

/**
 * WebRTC (in loopback) test with Selenium Grid.
 * </p>
 * * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint in loopback <br>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>No assertion, just data gathering</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcPerformanceLoopbackTest extends PerformanceTest {

  private static final String NUM_VIEWERS_PROPERTY = "perf.loopback.numviewers";
  private static final int NUM_VIEWERS_DEFAULT = 1;

  private static final String BROWSER_PER_VIEWER_PROPERTY = "perf.loopback.browserperviewer";
  private static final int BROWSER_PER_VIEWER_DEFAULT = 2;

  private static int numViewers;
  private static int browserPerViewer;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    numViewers = getProperty(NUM_VIEWERS_PROPERTY, NUM_VIEWERS_DEFAULT);
    browserPerViewer = getProperty(BROWSER_PER_VIEWER_PROPERTY, BROWSER_PER_VIEWER_DEFAULT);

    TestScenario test = new TestScenario();
    String video = getTestFilesDiskPath() + "/video/15sec/rgbHD.y4m";
    test.addBrowser(BrowserConfig.VIEWER,
        new Browser.Builder().webPageType(WebPageType.WEBRTC).numInstances(numViewers)
            .browserPerInstance(browserPerViewer).browserType(BrowserType.CHROME)
            .scope(BrowserScope.LOCAL).video(video).build());

    // Uncomment this for remote scenario
    // test.addBrowser(TestConfig.VIEWER, new BrowserClient.Builder()
    // .numInstances(numViewers).browserPerInstance(browserPerViewer)
    // .browserType(BrowserType.CHROME).scope(BrowserScope.REMOTE)
    // .video(video).build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testWebRtcPerformanceLoopback() throws Exception {
    Map<String, Browser> browsers = getTestScenario().getBrowserMap();

    final int playTime = ParallelBrowsers.getRampPlaytime(browsers.size());

    ParallelBrowsers.ramp(browsers, monitor, new BrowserRunner() {
      @Override
      public void run(Browser browser) throws Exception {

        long endTimeMillis = System.currentTimeMillis() + playTime;
        String name = browser.getId();
        MediaPipeline mp = null;

        try {
          // Media Pipeline
          mp = kurentoClient.createMediaPipeline();
          WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
          webRtcEndpoint.connect(webRtcEndpoint);

          log.debug(">>> start {}", name);
          getPage(name).subscribeEvents("playing");
          getPage(name).initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);

          monitor.addWebRtcClientAndActivateStats(getPage(name).getBrowser().getId(),
              webRtcEndpoint, getPage(name), "webRtcPeer.peerConnection");

          getPage(name).checkLatencyUntil(monitor, endTimeMillis);

        } catch (Throwable e) {
          log.error("[[[ {} ]]]", e.getCause().getMessage());
          throw e;
        } finally {
          log.debug("<<< finally {}", name);

          // Release Media Pipeline
          if (mp != null) {
            mp.release();
          }
        }
      }
    });
  }

}
