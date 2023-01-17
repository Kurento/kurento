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

package org.kurento.test.functional.dispatcher;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Dispatcher;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;

/**
 * A WebRtcEndpoint is connected to another WebRtcEndpoint through a Dispatcher
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>3 x Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Media server switchs the media from two WebRtcEndpoint using a Dispatcher, streaming
 * the result through antoher WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>The color of the received video should be as expected (green and the blue)</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time in remote video should be as expected</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherWebRtcTest extends FunctionalTest {

  private static final int PLAYTIME = 10; // seconds
  private static final String BROWSER1 = "browser1";
  private static final String BROWSER2 = "browser2";
  private static final String BROWSER3 = "browser3";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    TestScenario test = new TestScenario();

    test.addBrowser(BROWSER1, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    test.addBrowser(BROWSER2,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/green.y4m")
            .build());
    test.addBrowser(BROWSER3,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/blue.y4m")
            .build());

    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testDispatcherWebRtc() throws Exception {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEp1 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEp3 = new WebRtcEndpoint.Builder(mp).build();

    Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
    HubPort hubPort2 = new HubPort.Builder(dispatcher).build();
    HubPort hubPort3 = new HubPort.Builder(dispatcher).build();

    webRtcEp1.connect(hubPort1);
    webRtcEp3.connect(hubPort3);
    hubPort2.connect(webRtcEp2);

    dispatcher.connect(hubPort1, hubPort2);

    // Test execution
    getPage(BROWSER2).initWebRtc(webRtcEp1, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_ONLY);
    getPage(BROWSER3).initWebRtc(webRtcEp3, WebRtcChannel.VIDEO_ONLY, WebRtcMode.SEND_ONLY);

    getPage(BROWSER1).subscribeEvents("playing");
    getPage(BROWSER1).initWebRtc(webRtcEp2, WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);

    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER1).waitForEvent("playing"));

    Assert.assertTrue("The color of the video should be green",
        getPage(BROWSER1).similarColor(Color.GREEN));

    Thread.sleep(5000);
    dispatcher.connect(hubPort3, hubPort2);

    Assert.assertTrue("The color of the video should be blue (BLUE)",
        getPage(BROWSER1).similarColor(Color.BLUE));

    Thread.sleep(2000);

    // Release Media Pipeline
    mp.release();
  }
}
