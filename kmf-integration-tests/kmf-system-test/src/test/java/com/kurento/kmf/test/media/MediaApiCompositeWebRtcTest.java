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
import org.junit.Ignore;
import org.junit.Test;

import com.kurento.kmf.media.Composite;
import com.kurento.kmf.media.GStreamerFilter;
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
 * <strong>Description</strong>: Test of a Composite Mixer.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>4xWebRtcEndpoint -> Composite -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected (red, green, blue, and white)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiCompositeWebRtcTest extends BrowserMediaApiTest {

	@Ignore
	@Test
	public void testCompositeWebRtcChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Ignore
	@Test
	public void testCompositeWebRtcFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEP1 = mp.newWebRtcEndpoint().build();
		WebRtcEndpoint webRtcEP2 = mp.newWebRtcEndpoint().build();
		WebRtcEndpoint webRtcEP3 = mp.newWebRtcEndpoint().build();
		WebRtcEndpoint webRtcEP4 = mp.newWebRtcEndpoint().build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();

		Composite composite = mp.newComposite().build();
		HubPort hubPort1 = composite.newHubPort().build();
		HubPort hubPort2 = composite.newHubPort().build();
		HubPort hubPort3 = composite.newHubPort().build();
		HubPort hubPort4 = composite.newHubPort().build();
		HubPort hubPort5 = composite.newHubPort().build();

		webRtcEP1.connect(hubPort1);
		webRtcEP2.connect(hubPort2);
		webRtcEP3.connect(hubPort3);
		webRtcEP4.connect(hubPort4);
		hubPort5.connect(httpEP);

		// Test execution
		try (BrowserClient browserPlayer = new BrowserClient.Builder()
				.browser(browserType).client(Client.PLAYER).build();
				BrowserClient browserWebRtc1 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/red.y4m")
						.build();
				BrowserClient browserWebRtc2 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/green.y4m")
						.build();
				BrowserClient browserWebRtc3 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/blue.y4m")
						.build();
				BrowserClient browserWebRtc4 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/white.y4m")
						.build();) {

			// WebRTC browsers
			browserWebRtc1.subscribeEvents("playing");
			browserWebRtc1.connectToWebRtcEndpoint(webRtcEP1,
					WebRtcChannel.AUDIO_AND_VIDEO);
			browserWebRtc2.subscribeEvents("playing");
			browserWebRtc2.connectToWebRtcEndpoint(webRtcEP2,
					WebRtcChannel.AUDIO_AND_VIDEO);
			browserWebRtc3.subscribeEvents("playing");
			browserWebRtc3.connectToWebRtcEndpoint(webRtcEP3,
					WebRtcChannel.AUDIO_AND_VIDEO);
			browserWebRtc4.subscribeEvents("playing");
			browserWebRtc4.connectToWebRtcEndpoint(webRtcEP4,
					WebRtcChannel.AUDIO_AND_VIDEO);

			browserPlayer.setURL(httpEP.getUrl());
			browserPlayer.subscribeEvents("playing");
			browserPlayer.start();

			browserPlayer.setColorCoordinates(450, 450);

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browserPlayer.waitForEvent("playing"));
			Assert.assertTrue("Upper left part of the video must be red",
					browserPlayer.color(Color.RED, 12, 0, 0));
			Assert.assertTrue("Upper right part of the video must be green",
					browserPlayer.color(Color.GREEN, 14, 450, 0));
			Assert.assertTrue("Lower left part of the video must be blue",
					browserPlayer.color(Color.BLUE, 16, 0, 450));
			Assert.assertTrue("Lower right part of the video must be white",
					browserPlayer.color(Color.WHITE, 18, 450, 450));

			// Finally, a B&N filter is connected in one of the WebRTC's
			GStreamerFilter bn = mp.newGStreamerFilter(
					"videobalance saturation=0.0").build();
			webRtcEP1.connect(bn);
			bn.connect(hubPort1);
			Thread.sleep(5000);
			Assert.assertTrue(
					"When connecting the filter, the upper left part of the video must be gray",
					browserPlayer.color(new Color(75, 75, 75), 25, 0, 0));
		}
	}

}
