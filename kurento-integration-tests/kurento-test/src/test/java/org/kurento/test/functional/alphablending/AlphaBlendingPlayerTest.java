/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.functional.alphablending;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.AlphaBlending;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * 
 * <strong>Description</strong>: Three synthetic videos are played by four
 * PlayerEndpoint and mixed by a AlphaBlending. The resulting video is played in
 * an WebRtcEndpoint.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>3xPlayerEndpoint -> AlphaBlending -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected (red, green, blue)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.1.0
 */
public class AlphaBlendingPlayerTest extends FunctionalTest {

	private static int PLAYTIME = 5; // seconds

	@Test
	public void testCompositePlayerChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testCompositePlayerFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/red.webm").build();
		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/green.webm").build();
		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/blue.webm").build();

		AlphaBlending alphaBlending = new AlphaBlending.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort2 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort3 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort4 = new HubPort.Builder(alphaBlending).build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		playerRed.connect(hubPort1);
		playerGreen.connect(hubPort2);
		playerBlue.connect(hubPort3);

		hubPort4.connect(webRtcEP);

		alphaBlending.setMaster(hubPort1, 1);

		alphaBlending.setPortProperties(0F, 0F, 8, 0.2F, 0.2F, hubPort2);
		alphaBlending.setPortProperties(0.4F, 0.4F, 7, 0.2F, 0.2F, hubPort3);

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			playerRed.play();
			playerGreen.play();
			playerBlue.play();

			Thread.sleep(2000);
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));

			Thread.sleep(2000);

			// Assertions
			Assert.assertTrue("Upper left part of the video must be blue",
					browser.similarColorAt(Color.GREEN, 0, 0));
			Assert.assertTrue("Lower right part of the video must be red",
					browser.similarColorAt(Color.RED, 315, 235));
			Assert.assertTrue("Center of the video must be blue",
					browser.similarColorAt(Color.BLUE, 160, 120));

			// alphaBlending.setMaster(hubPort3, 1);
			alphaBlending
					.setPortProperties(0.8F, 0.8F, 7, 0.2F, 0.2F, hubPort3);

			Assert.assertTrue("Lower right part of the video must be blue",
					browser.similarColorAt(Color.BLUE, 315, 235));
			Assert.assertTrue("Center of the video must be red",
					browser.similarColorAt(Color.RED, 160, 120));

			Thread.sleep(PLAYTIME * 1000);
		}

		// Release Media Pipeline
		mp.release();
	}

}
