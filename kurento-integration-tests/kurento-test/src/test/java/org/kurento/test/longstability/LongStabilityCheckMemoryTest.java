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

package org.kurento.test.longstability;

import static org.kurento.test.config.TestConfiguration.TEST_DURATION_PROPERTY;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ServerManager;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.PropertiesManager;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.monitor.SystemMonitorManager;

/**
 * Memory stability test for KMS.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpointSender -> WebRtcEndpoint (loopback)</li>
 * <li>60 fakes WebRtcEndpoints connect to WebRtcEndpointSender</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) WebRtcEndpoint in loopback.</li>
 * <li>(Browser) WebRtcPeer in send-receive mode sends and receives media</li>
 * <li>60 fakes WebRtcEndpoints connect to WebRtcEndpointSender</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Verify that the memory doesn't increase more than 50 %</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */
public class LongStabilityCheckMemoryTest extends StabilityTest {

  private double CONSTANT_MB = 0.000124;
  private int FAKE_CLIENT_NUM = 30;

  @Override
  public void setupBrowserTest() throws InterruptedException {
    super.setupBrowserTest();
    fakeKurentoClient = fakeKms.getKurentoClient();
    Assert.assertNotNull("Fake Kurento Client is null", fakeKurentoClient);
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    String videoPath = KurentoTest.getTestFilesDiskPath() + "/video/15sec/rgbHD.y4m";
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.BROWSER, new Browser.Builder().webPageType(WebPageType.WEBRTC)
        .browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).video(videoPath).build());

    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testLongStabilityCheckMemory() throws Exception {
    int i = 0;
    long initMemory = 0;
    long increaseMemory = 0;
    long firstIncreaseMemory = 0;
    long endMemory = 0;
    long diffMemory = 0;
    double percentageMemory = 0.0;
    Date date = null;

    ServerManager serverManager = kurentoClient.getServerManager();
    MediaPipeline mp = null;
    WebRtcEndpoint webRtcEndpoint = null;

    long testDurationMillis =
        PropertiesManager.getProperty(TEST_DURATION_PROPERTY, DEFAULT_TEST_DURATION);

    endTestTime = System.currentTimeMillis() + testDurationMillis;

    SystemMonitorManager monitor = new SystemMonitorManager();
    monitor.startMonitoring();
    PrintWriter writer =
        new PrintWriter(getDefaultOutputFolder() + File.separator + "increaseMemory.txt", "UTF-8");

    initMemory = (long) (serverManager.getUsedMemory() * CONSTANT_MB);

    try {
      while (!isTimeToFinishTest()) {
        log.debug("Memory init: {} Mb", initMemory);
        // Media Pipeline
        mp = kurentoClient.createMediaPipeline();
        webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
        webRtcEndpoint.connect(webRtcEndpoint);

        // WebRTC
        getPage().subscribeEvents("playing");
        getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_RCV);

        // Add fakes
        addFakeClients(FAKE_CLIENT_NUM, mp, webRtcEndpoint, 600, monitor, null);

        Thread.sleep(30000);
        // Release all Media Pipeline and Fakes
        fakeKms.releaseAllFakePipelines(500, monitor);
        mp.release();
        Thread.sleep(10000);

        log.debug("Iteration: {}. KMS has {} pipelines", i, serverManager.getPipelines().size());
        endMemory = (long) (serverManager.getUsedMemory() * CONSTANT_MB);
        diffMemory = Math.abs(endMemory - initMemory);
        increaseMemory = (diffMemory - increaseMemory) + increaseMemory;
        if (i == 0) {
          firstIncreaseMemory = increaseMemory;
        }
        percentageMemory = ((increaseMemory - firstIncreaseMemory) * 100) / firstIncreaseMemory;

        log.debug("Iteration {}. End Memory: {} Mb", i, endMemory);
        log.debug("Iteration {}. Diff Memory: {} Mb", i, diffMemory);
        log.debug("Iteration {}. Increase Memory: {} Mb. % Increase: {} %", i, increaseMemory,
            percentageMemory);

        date = new Date();

        writer.println(
            date.toString() + " Iteration " + i + ". KMS has " + serverManager.getPipelines().size()
                + " pipelines. Init Memory: " + initMemory + " Mb. End Memory: " + endMemory
                + " Mb. Diff Memory: " + diffMemory + " Mb. Increase Memory: " + increaseMemory
                + " Mb. % Increase: " + percentageMemory + "%");
        i++;

        Assert.assertTrue(
            "The memory increases more than 50%. The percentage memory was " + percentageMemory,
            percentageMemory < 50.0);
        mp = null;
        webRtcEndpoint = null;
      }
    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      writer.close();
    }
  }

}
