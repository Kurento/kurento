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
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * 
 * <strong>Description</strong>: Three synthetic videos are played by three
 * WebRtcEndpoint and mixed by an AlphaBlending. The resulting video is played
 * in an WebRtcEndpoint.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>3xWebRtcEndpoint -> AlphaBlending -> WebRtcEndpoint</li>
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
public class AlphaBlendingWebRtcTest extends FunctionalTest {

	private static int PLAYTIME = 5;

	@Test
	public void testCompositeWebRtcChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEPRed = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPGreen = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPBlue = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPAlphabaBlending = new WebRtcEndpoint.Builder(mp)
				.build();

		AlphaBlending alphaBlending = new AlphaBlending.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort2 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort3 = new HubPort.Builder(alphaBlending).build();
		HubPort hubPort4 = new HubPort.Builder(alphaBlending).build();

		webRtcEPRed.connect(hubPort1);
		webRtcEPGreen.connect(hubPort2);
		webRtcEPBlue.connect(hubPort3);
		hubPort4.connect(webRtcEPAlphabaBlending);

		alphaBlending.setMaster(hubPort1, 1);

		alphaBlending.setPortProperties(0F, 0F, 8, 0.2F, 0.2F, hubPort2);
		alphaBlending.setPortProperties(0.4F, 0.4F, 7, 0.2F, 0.2F, hubPort3);

		// Test execution
		try (BrowserClient browserAlphaBlending = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build();

				BrowserClient browserRed = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/red.y4m")
						.build();
				BrowserClient browserGreen = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/green.y4m")
						.build();
				BrowserClient browserBlue = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/blue.y4m")
						.build();) {

			// WebRTC browsers
			browserRed.initWebRtc(webRtcEPRed, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_ONLY);
			browserGreen.initWebRtc(webRtcEPGreen,
					WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
			browserBlue.initWebRtc(webRtcEPBlue, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_ONLY);

			browserAlphaBlending.subscribeEvents("playing");
			browserAlphaBlending.initWebRtc(webRtcEPAlphabaBlending,
					WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

			// Assertions
			Assert.assertTrue("Upper left part of the video must be blue",
					browserAlphaBlending.similarColorAt(Color.GREEN, 0, 0));
			Assert.assertTrue("Lower right part of the video must be red",
					browserAlphaBlending.similarColorAt(Color.RED, 315, 235));
			Assert.assertTrue("Center of the video must be blue",
					browserAlphaBlending.similarColorAt(Color.BLUE, 160, 120));

			// alphaBlending.setMaster(hubPort3, 1);
			alphaBlending
					.setPortProperties(0.8F, 0.8F, 7, 0.2F, 0.2F, hubPort3);

			Assert.assertTrue("Lower right part of the video must be blue",
					browserAlphaBlending.similarColorAt(Color.BLUE, 315, 235));
			Assert.assertTrue("Center of the video must be red",
					browserAlphaBlending.similarColorAt(Color.RED, 160, 120));
			Thread.sleep(PLAYTIME * 1000);
		}

		// Release Media Pipeline
		mp.release();
	}

}
