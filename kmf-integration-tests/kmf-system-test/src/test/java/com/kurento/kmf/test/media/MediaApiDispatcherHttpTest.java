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
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.HubPort;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.WebRtcChannel;

/**
 * 
 * <strong>Description</strong>: Test of a Dispatcher Mixer.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> Dispatcher -> HttpGetEndpoint</li>
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
public class MediaApiDispatcherHttpTest extends BrowserMediaApiTest {

	@Test
	public void testDispatcherHttpChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEP1 = mp.newWebRtcEndpoint().build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();

		Dispatcher dispatcher = mp.newDispatcher().build();
		HubPort hubPort1 = dispatcher.newHubPort().build();
		HubPort hubPort2 = dispatcher.newHubPort().build();

		webRtcEP1.connect(hubPort1);
		hubPort2.connect(httpEP);

		dispatcher.connect(hubPort1, hubPort2);

		// Test execution
		try (BrowserClient browser1 = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build();
				BrowserClient browser2 = new BrowserClient.Builder()
						.browser(browserType).client(Client.PLAYER).build();) {

			browser1.connectToWebRtcEndpoint(webRtcEP1,
					WebRtcChannel.AUDIO_AND_VIDEO);

			browser2.setURL(httpEP.getUrl());
			browser2.subscribeEvents("playing");
			browser2.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser2.waitForEvent("playing"));
			Assert.assertTrue("The color of the video should be green",
					browser2.colorSimilarTo(new Color(0, 135, 0)));
			Thread.sleep(5000);
		}
	}
}
