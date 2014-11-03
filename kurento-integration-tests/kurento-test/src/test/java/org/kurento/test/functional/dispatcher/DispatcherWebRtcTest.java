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
package org.kurento.test.functional.dispatcher;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.Dispatcher;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;

/**
 * 
 * <strong>Description</strong>: A WebRtcEndpoint is connected to another
 * WebRtcEndpoint through a Dispatcher.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherWebRtcTest extends BrowserKurentoClientTest {

	private static final int PLAYTIME = 10; // seconds

	@Test
	public void testDispatcherWebRtcChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = new MediaPipeline.Builder(kurentoClient).build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();

		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();

		webRtcEP1.connect(hubPort1);
		hubPort2.connect(webRtcEP2);

		dispatcher.connect(hubPort1, hubPort2);

		// Test execution
		try (BrowserClient browser1 = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build();
				BrowserClient browser2 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC).build();) {

			browser1.initWebRtc(webRtcEP1, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_ONLY);

			browser2.subscribeEvents("playing");
			browser2.initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			Thread.sleep(PLAYTIME * 1000);

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser2.waitForEvent("playing"));
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					browser2.similarColor(new Color(0, 135, 0)));
		}

		// Release Media Pipeline
		mp.release();
	}
}
