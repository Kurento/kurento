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

package org.kurento.test.functional.composite;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Composite;
import org.kurento.client.GStreamerFilter;
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
 * Four synthetic videos are played by four WebRtcEndpoint and mixed by a Composite. The resulting
 * video is played in an WebRtcEndpoint. At the end, a B&N filter is connected in one of the
 * WebRTC's
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>4xWebRtcEndpoint -> Composite -> WebRtcEndpoint</li>
 * <li>1xWebRtcEndpoint -> GStreamerFilter</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>5 x Chrome</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Media server implements a grid with the media from 4 WebRtcEndpoints and sends the
 * resulting media to another WebRtcEndpoint. Then the media of one the WebRtcEndpoint is filtered
 * with GStreamerFilter (B&N video)</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color of the video should be the expected in the right position (grid)</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class CompositeWebRtcTest extends FunctionalTest {

  private static final String BROWSER1 = "browser1";
  private static final String BROWSER2 = "browser2";
  private static final String BROWSER3 = "browser3";
  private static final String BROWSER4 = "browser4";
  private static final String BROWSER5 = "browser5";

  private static int PLAYTIME = 5;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    // Test: 5 local Chrome's
    TestScenario test = new TestScenario();
    test.addBrowser(BROWSER1, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    test.addBrowser(BROWSER2,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/red.y4m")
            .build());
    test.addBrowser(BROWSER3,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/green.y4m")
            .build());
    test.addBrowser(BROWSER4,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/blue.y4m")
            .build());
    test.addBrowser(BROWSER5,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesDiskPath() + "/video/10sec/white.y4m")
            .build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testCompositeWebRtc() throws Exception {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEpRed = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEpGreen = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEpBlue = new WebRtcEndpoint.Builder(mp).build();

    Composite composite = new Composite.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(composite).build();
    HubPort hubPort2 = new HubPort.Builder(composite).build();
    HubPort hubPort3 = new HubPort.Builder(composite).build();

    webRtcEpRed.connect(hubPort1);
    webRtcEpGreen.connect(hubPort2);
    webRtcEpBlue.connect(hubPort3);

    WebRtcEndpoint webRtcEpWhite = new WebRtcEndpoint.Builder(mp).build();
    HubPort hubPort4 = new HubPort.Builder(composite).build();
    webRtcEpWhite.connect(hubPort4);

    WebRtcEndpoint webRtcEpComposite = new WebRtcEndpoint.Builder(mp).build();
    HubPort hubPort5 = new HubPort.Builder(composite).build();
    hubPort5.connect(webRtcEpComposite);

    // WebRTC browsers
    getPage(BROWSER2).initWebRtc(webRtcEpRed, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(BROWSER3).initWebRtc(webRtcEpGreen, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);
    getPage(BROWSER4).initWebRtc(webRtcEpBlue, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(BROWSER5).initWebRtc(webRtcEpWhite, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);

    getPage(BROWSER1).subscribeEvents("playing");
    getPage(BROWSER1).initWebRtc(webRtcEpComposite, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.RCV_ONLY);

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER1).waitForEvent("playing"));
    Assert.assertTrue("Upper left part of the video must be red",
        getPage(BROWSER1).similarColorAt(Color.RED, 0, 0));
    Assert.assertTrue("Upper right part of the video must be green",
        getPage(BROWSER1).similarColorAt(Color.GREEN, 450, 0));
    Assert.assertTrue("Lower left part of the video must be blue",
        getPage(BROWSER1).similarColorAt(Color.BLUE, 0, 450));
    Assert.assertTrue("Lower right part of the video must be white",
        getPage(BROWSER1).similarColorAt(Color.WHITE, 450, 450));

    // Finally, a black & white filter is connected to one WebRTC
    GStreamerFilter bwFilter =
        new GStreamerFilter.Builder(mp, "videobalance saturation=0.0").build();
    webRtcEpRed.connect(bwFilter);
    bwFilter.connect(hubPort1);
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));
    Assert.assertTrue("When connecting the filter, the upper left part of the video must be gray",
        getPage(BROWSER1).similarColorAt(new Color(75, 75, 75), 0, 0));

    // Release Media Pipeline
    mp.release();
  }

}
