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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.DispatcherOneToMany;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
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
 * <strong>Description</strong>: A PlayerEndpoint is connected to a
 * WebRtcEndpoint through a Dispatcher.<br/>
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
 * @since 4.2.3
 */
public class DispatcherOneToManyPlayerTest extends FunctionalTest {

	private static final int PLAYTIME = 30; // seconds
	private static final int TIMEOUT_EOS = 60; // seconds

	@Test
	public void testDispatcherOneToManyPlayerChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Test
	public void testDispatcherOneToManyPlayerFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/red.webm").build();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/30sec/blue.webm").build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEP3 = new WebRtcEndpoint.Builder(mp).build();

		DispatcherOneToMany dispatcherOneToMany = new DispatcherOneToMany.Builder(
				mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort3 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort4 = new HubPort.Builder(dispatcherOneToMany).build();
		HubPort hubPort5 = new HubPort.Builder(dispatcherOneToMany).build();

		playerEP.connect(hubPort1);
		playerEP2.connect(hubPort2);
		hubPort3.connect(webRtcEP1);
		hubPort4.connect(webRtcEP2);
		hubPort5.connect(webRtcEP3);
		dispatcherOneToMany.setSource(hubPort1);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution
		try (BrowserClient browser1 = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build();
				BrowserClient browser2 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC).build();
				BrowserClient browser3 = new BrowserClient.Builder()
						.browser(browserType).client(Client.WEBRTC).build()) {

			browser1.subscribeEvents("playing");
			browser1.initWebRtc(webRtcEP1, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			browser2.subscribeEvents("playing");
			browser2.initWebRtc(webRtcEP2, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			browser3.subscribeEvents("playing");
			browser3.initWebRtc(webRtcEP3, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			playerEP.play();

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

			Assert.assertTrue("The color of the video should be red",
					browser1.similarColor(Color.RED));
			Assert.assertTrue("The color of the video should be red",
					browser2.similarColor(Color.RED));
			Assert.assertTrue("The color of the video should be red",
					browser3.similarColor(Color.RED));

			Thread.sleep(3000);
			playerEP2.play();
			dispatcherOneToMany.setSource(hubPort2);

			Assert.assertTrue("The color of the video should be blue",
					browser1.similarColor(Color.BLUE));
			Assert.assertTrue("The color of the video should be blue",
					browser2.similarColor(Color.BLUE));
			Assert.assertTrue("The color of the video should be blue",
					browser3.similarColor(Color.BLUE));

			Thread.sleep(3000);
			dispatcherOneToMany.setSource(hubPort1);

			Assert.assertTrue("The color of the video should be red",
					browser1.similarColor(Color.RED));
			Assert.assertTrue("The color of the video should be red",
					browser2.similarColor(Color.RED));
			Assert.assertTrue("The color of the video should be red",
					browser3.similarColor(Color.RED));

			Thread.sleep(3000);

			dispatcherOneToMany.setSource(hubPort2);

			Assert.assertTrue("The color of the video should be red",
					browser1.similarColor(Color.BLUE));
			Assert.assertTrue("The color of the video should be red",
					browser2.similarColor(Color.BLUE));
			Assert.assertTrue("The color of the video should be red",
					browser3.similarColor(Color.BLUE));

			Thread.sleep(3000);

			Assert.assertTrue("Not received EOS event in player",
					eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));

			double currentTime = browser1.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));
		} finally {
			// Release Media Pipeline
			mp.release();
		}
	}
}
