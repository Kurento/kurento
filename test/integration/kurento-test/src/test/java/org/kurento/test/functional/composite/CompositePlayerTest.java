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
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Composite;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Four synthetic videos are played by four PlayerEndpoint and mixed by a Composite. The resulting
 * video is played in an WebRtcEndpoint
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>4xPlayerEndpoint -> Composite -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Media server implements a grid with the media from 4 PlayerEndpoints</li>
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
public class CompositePlayerTest extends FunctionalTest {

  private static int PLAYTIME = 5; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testCompositePlayer() throws Exception {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    PlayerEndpoint playerRed =
        new PlayerEndpoint.Builder(mp, "http://" + getTestFilesHttpPath() + "/video/30sec/red.webm")
            .build();
    PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/green.webm").build();
    PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/blue.webm").build();

    Composite composite = new Composite.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(composite).build();
    HubPort hubPort2 = new HubPort.Builder(composite).build();
    HubPort hubPort3 = new HubPort.Builder(composite).build();

    playerRed.connect(hubPort1);
    playerGreen.connect(hubPort2);
    playerBlue.connect(hubPort3);

    PlayerEndpoint playerWhite = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/white.webm").build();
    HubPort hubPort4 = new HubPort.Builder(composite).build();
    playerWhite.connect(hubPort4);

    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    HubPort hubPort5 = new HubPort.Builder(composite).build();
    hubPort5.connect(webRtcEp);

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

    playerRed.play();
    playerGreen.play();
    playerBlue.play();
    playerWhite.play();

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));
    Assert.assertTrue("Upper left part of the video must be red",
        getPage().similarColorAt(Color.RED, 0, 0));
    Assert.assertTrue("Upper right part of the video must be green",
        getPage().similarColorAt(Color.GREEN, 450, 0));
    Assert.assertTrue("Lower left part of the video must be blue",
        getPage().similarColorAt(Color.BLUE, 0, 450));
    Assert.assertTrue("Lower right part of the video must be white",
        getPage().similarColorAt(Color.WHITE, 450, 450));

    // Guard time to see the composite result
    Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

    // Release Media Pipeline
    mp.release();
  }

}
