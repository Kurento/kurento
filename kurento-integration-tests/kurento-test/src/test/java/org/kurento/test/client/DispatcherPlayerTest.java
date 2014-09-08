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
package org.kurento.test.client;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.kurento.client.Dispatcher;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;

/**
 * 
 * <strong>Description</strong>: A PlayerEndpoint is connected to a
 * WebRtcEndpoint through a Dispatcher.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherPlayerTest extends BrowserKurentoClientTest {

	@Test
	public void testDispatcherPlayerChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Ignore
	@Test
	public void testDispatcherFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/red.webm").build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();

		playerEP.connect(hubPort1);
		hubPort2.connect(webRtcEP);

		dispatcher.connect(hubPort1, hubPort2);
		playerEP.play();

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {

			browser.subscribeEvents("playing");
			browser.connectToWebRtcEndpoint(webRtcEP,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Thread.sleep(10000);
			Assert.assertTrue("The color of the video should be red",
					browser.colorSimilarTo(Color.RED));
		}
	}
}
