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
package com.kurento.kmf.test.media;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.Dispatcher;
import com.kurento.kmf.media.HubPort;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.WebRtcChannel;

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
public class MediaApiDispatcherPlayerTest extends BrowserMediaApiTest {

	@Test
	public void testDispatcherPlayerChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testDispatcherFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();

		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/30sec/red.webm").build();
		WebRtcEndpoint webRtcEP = mp.newWebRtcEndpoint().build();

		Dispatcher dispatcher = mp.newDispatcher().build();
		HubPort hubPort1 = dispatcher.newHubPort().build();
		HubPort hubPort2 = dispatcher.newHubPort().build();

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
