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
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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
import org.kurento.test.latency.LatencyController;

/**
 * WebRTC (one to many) test with Selenium Grid.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> N x WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint one to many</li>
 * <li>(Browser) N WebRtcPeers in rcv-only receive media</li>
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
public class WebRtcPerformanceOneToManyTest extends PerformanceTest {

  private static final String NUM_VIEWERS_PROPERTY = "perf.one2many.numviewers";
  private static final int NUM_VIEWERS_DEFAULT = 1;

  private static final String BROWSER_PER_VIEWER_PROPERTY = "perf.one2many.browserperviewer";
  private static final int BROWSER_PER_VIEWER_DEFAULT = 2;

  private static int numViewers;
  private static int browserPerViewer;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    numViewers = getProperty(NUM_VIEWERS_PROPERTY, NUM_VIEWERS_DEFAULT);
    browserPerViewer = getProperty(BROWSER_PER_VIEWER_PROPERTY, BROWSER_PER_VIEWER_DEFAULT);

    TestScenario test = new TestScenario();
    String video = getTestFilesDiskPath() + "/video/15sec/rgbHD.y4m";
    test.addBrowser(BrowserConfig.PRESENTER, new Browser.Builder().webPageType(WebPageType.WEBRTC)
        .browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).video(video).build());

    test.addBrowser(BrowserConfig.VIEWER,
        new Browser.Builder().webPageType(WebPageType.WEBRTC).numInstances(numViewers)
            .browserPerInstance(browserPerViewer).browserType(BrowserType.CHROME)
            .scope(BrowserScope.LOCAL).build());

    // Uncomment this for remote scenario
    // test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
    // .browserType(BrowserType.CHROME).scope(BrowserScope.REMOTE)
    // .video(video).build());
    //
    // ... or saucelabs, for example:
    //
    // test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
    // .browserType(BrowserType.CHROME).scope(BrowserScope.SAUCELABS)
    // .platform(Platform.WIN8_1).browserVersion("39").build());
    //
    // test.addBrowser(TestConfig.VIEWER, new BrowserClient.Builder()
    // .numInstances(numViewers).browserPerInstance(browserPerViewer)
    // .browserType(BrowserType.CHROME).scope(BrowserScope.REMOTE)
    // .build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testWebRtcPerformanceOneToMany() throws InterruptedException {
    // Media Pipeline
    final MediaPipeline mp = kurentoClient.createMediaPipeline();
    final WebRtcEndpoint masterWebRtcEp = new WebRtcEndpoint.Builder(mp).build();

    // Master
    getPresenter().subscribeLocalEvents("playing");
    getPresenter().initWebRtc(masterWebRtcEp, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_ONLY);

    Map<String, Browser> browsers = new TreeMap<>(getTestScenario().getBrowserMap());
    browsers.remove(BrowserConfig.PRESENTER);
    final int playTime = ParallelBrowsers.getRampPlaytime(browsers.size());

    ParallelBrowsers.ramp(browsers, monitor, new BrowserRunner() {
      @Override
      public void run(Browser browser) throws Exception {
        String name = browser.getId();

        try {
          // Viewer
          WebRtcEndpoint viewerWebRtcEp = new WebRtcEndpoint.Builder(mp).build();
          masterWebRtcEp.connect(viewerWebRtcEp);

          // Latency control
          LatencyController cs = new LatencyController("Latency control on " + name, monitor);

          // WebRTC
          log.debug(">>> start {}", name);
          getPage(name).subscribeEvents("playing");
          getPage(name).initWebRtc(viewerWebRtcEp, WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);

          // Latency assessment
          cs.checkLatency(playTime, TimeUnit.MILLISECONDS, getPresenter(), getPage(name));

        } catch (Throwable e) {
          log.error("[[[ {} ]]]", e.getCause().getMessage());
          throw e;
        }
      }
    });

    log.debug("<<< Releasing pipeline");

    // Release Media Pipeline
    if (mp != null) {
      mp.release();
    }
  }
}
