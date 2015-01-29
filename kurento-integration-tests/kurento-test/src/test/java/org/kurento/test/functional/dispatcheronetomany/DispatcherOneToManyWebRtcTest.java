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
package org.kurento.test.functional.dispatcheronetomany;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.DispatcherOneToMany;
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
public class DispatcherOneToManyWebRtcTest extends FunctionalTest {

	private static final int PLAYTIME = 5; // seconds

	@Test
	public void testDispatcherWebRtcChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP3 = new WebRtcEndpoint.Builder(mp).build();

		DispatcherOneToMany dispatcherOneToMany = new DispatcherOneToMany.Builder(
				mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort3 = new HubPort.Builder(dispatcherOneToMany).build();

		webRtcEP1.connect(hubPort1);
		webRtcEP2.connect(hubPort2);
		webRtcEP3.connect(hubPort3);
		hubPort1.connect(webRtcEP1);
		hubPort2.connect(webRtcEP2);
		hubPort3.connect(webRtcEP3);

		dispatcherOneToMany.setSource(hubPort1);

		// Test execution
		try (BrowserClient browser1 = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC)
				.video(getPathTestFiles() + "/video/10sec/green.y4m").build();
				BrowserClient browser2 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/blue.y4m")
						.build();
				BrowserClient browser3 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC)
						.video(getPathTestFiles() + "/video/10sec/red.y4m")
						.build();) {

			browser1.subscribeEvents("playing");
			browser1.initWebRtc(webRtcEP1, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			browser2.subscribeEvents("playing");
			browser2.initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			browser3.subscribeEvents("playing");
			browser3.initWebRtc(webRtcEP3, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.SEND_RCV);

			Thread.sleep(PLAYTIME * 1000);

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser1.waitForEvent("playing"));
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser2.waitForEvent("playing"));
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser3.waitForEvent("playing"));

			Assert.assertTrue("The color of the video should be green (GREEN)",
					browser1.similarColorAt(Color.GREEN, 450, 0));
			Assert.assertTrue("The color of the video should be green (GREEN)",
					browser2.similarColorAt(Color.GREEN, 450, 0));
			Assert.assertTrue("The color of the video should be green (GREEN)",
					browser3.similarColorAt(Color.GREEN, 450, 0));

			Thread.sleep(3000);
			dispatcherOneToMany.setSource(hubPort2);

			Assert.assertTrue("The color of the video should be blue (BLUE)",
					browser1.similarColorAt(Color.BLUE, 450, 0));
			Assert.assertTrue("The color of the video should be blue (BLUE)",
					browser2.similarColorAt(Color.BLUE, 450, 0));
			Assert.assertTrue("The color of the video should be blue (BLUE)",
					browser3.similarColorAt(Color.BLUE, 450, 0));

			Thread.sleep(3000);
			dispatcherOneToMany.setSource(hubPort3);

			Assert.assertTrue("The color of the video should be red (RED)",
					browser1.similarColorAt(Color.RED, 450, 0));
			Assert.assertTrue("The color of the video should be red (RED)",
					browser2.similarColorAt(Color.RED, 450, 0));
			Assert.assertTrue("The color of the video should be red (RED)",
					browser3.similarColorAt(Color.RED, 450, 0));

			Thread.sleep(3000);
			dispatcherOneToMany.removeSource();
			Assert.assertTrue("The color of the video should be red (RED)",
					browser1.similarColorAt(Color.RED, 450, 0));
			Assert.assertTrue("The color of the video should be red (RED)",
					browser2.similarColorAt(Color.RED, 450, 0));
			Assert.assertTrue("The color of the video should be red (RED)",
					browser3.similarColorAt(Color.RED, 450, 0));

			Thread.sleep(2000);

		} finally {
			// Release Media Pipeline
			mp.release();
		}
	}
}
