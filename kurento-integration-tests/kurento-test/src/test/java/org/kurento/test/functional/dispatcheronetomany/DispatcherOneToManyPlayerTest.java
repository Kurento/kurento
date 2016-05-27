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

package org.kurento.test.functional.dispatcheronetomany;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.DispatcherOneToMany;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
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
 *
 * <strong>Description</strong>: A PlayerEndpoint is connected to a WebRtcEndpoint through a
 * Dispatcher.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time should be the expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.0.0
 */

public class DispatcherOneToManyPlayerTest extends FunctionalTest {
  private static final int TIMEOUT_EOS = 60; // seconds
  private static final String BROWSER1 = "browser1";
  private static final String BROWSER2 = "browser2";
  private static final String BROWSER3 = "browser3";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    TestScenario test = new TestScenario();

    test.addBrowser(BROWSER1, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    test.addBrowser(BROWSER2, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    test.addBrowser(BROWSER3, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());

    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testDispatcherOneToManyPlayer() throws Exception {
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    PlayerEndpoint playerEp =
        new PlayerEndpoint.Builder(mp, "http://" + getTestFilesHttpPath() + "/video/30sec/red.webm")
            .build();
    PlayerEndpoint playerEp2 = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/blue.webm").build();
    WebRtcEndpoint webRtcEp1 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEp3 = new WebRtcEndpoint.Builder(mp).build();

    DispatcherOneToMany dispatcherOneToMany = new DispatcherOneToMany.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(dispatcherOneToMany).build();
    HubPort hubPort2 = new HubPort.Builder(dispatcherOneToMany).build();
    HubPort hubPort3 = new HubPort.Builder(dispatcherOneToMany).build();
    HubPort hubPort4 = new HubPort.Builder(dispatcherOneToMany).build();
    HubPort hubPort5 = new HubPort.Builder(dispatcherOneToMany).build();

    playerEp.connect(hubPort1);
    playerEp2.connect(hubPort2);
    hubPort3.connect(webRtcEp1);
    hubPort4.connect(webRtcEp2);
    hubPort5.connect(webRtcEp3);
    dispatcherOneToMany.setSource(hubPort1);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });

    // Test execution
    getPage(BROWSER1).subscribeEvents("playing");
    getPage(BROWSER1).initWebRtc(webRtcEp2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    getPage(BROWSER2).subscribeEvents("playing");
    getPage(BROWSER2).initWebRtc(webRtcEp1, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    getPage(BROWSER3).subscribeEvents("playing");
    getPage(BROWSER3).initWebRtc(webRtcEp3, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    playerEp.play();

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER1).waitForEvent("playing"));
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER2).waitForEvent("playing"));
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(BROWSER3).waitForEvent("playing"));

    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER1).similarColor(Color.RED));
    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER2).similarColor(Color.RED));
    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER3).similarColor(Color.RED));

    Thread.sleep(3000);
    playerEp2.play();
    dispatcherOneToMany.setSource(hubPort2);

    Assert.assertTrue("The color of the video should be blue",
        getPage(BROWSER1).similarColor(Color.BLUE));
    Assert.assertTrue("The color of the video should be blue",
        getPage(BROWSER2).similarColor(Color.BLUE));
    Assert.assertTrue("The color of the video should be blue",
        getPage(BROWSER3).similarColor(Color.BLUE));

    Thread.sleep(3000);
    dispatcherOneToMany.setSource(hubPort1);

    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER1).similarColor(Color.RED));
    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER2).similarColor(Color.RED));
    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER3).similarColor(Color.RED));

    Thread.sleep(3000);

    dispatcherOneToMany.setSource(hubPort2);

    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER1).similarColor(Color.BLUE));
    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER2).similarColor(Color.BLUE));
    Assert.assertTrue("The color of the video should be red",
        getPage(BROWSER3).similarColor(Color.BLUE));

    Thread.sleep(3000);

    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));
  }
}
