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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.WebRtcChannel;

/**
 * <strong>Description</strong>: Back-to-back WebRTC Test<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * <li>WebRtcEndpoint -> HttpEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browsers starts before 60 seconds (default timeout)</li>
 * <li>HttpPlayer play time does not differ in a 10% of the transmitting time by
 * WebRTC</li>
 * <li>Color received by HttpPlayer should be green (RGB #008700, video test of
 * Chrome)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiWebRtc2HttpTest extends BrowserMediaApiTest {

	private static int PLAYTIME = 5; // seconds to play in HTTP player
	private static int NPLAYERS = 2; // number of HttpEndpoint connected to

	// WebRTC source

	@Test
	@Ignore
	public void testWebRtc2Http() throws Exception {
		// Media Pipeline
		final MediaPipeline mp = pipelineFactory.create();
		final WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		webRtcEndpoint.connect(webRtcEndpoint);

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.WEBRTC).build()) {
			browser.subscribeEvents("playing");
			browser.connectToWebRtcEndpoint(webRtcEndpoint,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Wait until event playing in the WebRTC remote stream
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// HTTP Players
			ExecutorService exec = Executors.newFixedThreadPool(NPLAYERS);
			List<Future<?>> results = new ArrayList<>();
			for (int i = 0; i < NPLAYERS; i++) {
				results.add(exec.submit(new Runnable() {
					@Override
					public void run() {
						HttpGetEndpoint httpEP = mp.newHttpGetEndpoint()
								.build();
						webRtcEndpoint.connect(httpEP);
						try {
							createPlayer(httpEP.getUrl());
						} catch (InterruptedException e) {
							Assert.fail("Exception creating http players: "
									+ e.getClass().getName());
						}
					}
				}));
			}
			for (Future<?> r : results) {
				r.get();
			}
		}
	}

	private void createPlayer(String url) throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.PLAYER).build()) {
			browser.setURL(url);
			browser.subscribeEvents("playing");
			browser.start();

			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// Guard time to see the video
			Thread.sleep(PLAYTIME * 1000);

			// Assertions
			Assert.assertTrue("Error in play time of HTTP player (expected: "
					+ PLAYTIME + " sec, real: " + browser.getCurrentTime()
					+ " sec)", compare(PLAYTIME, browser.getCurrentTime()));
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					browser.colorSimilarTo(new Color(0, 135, 0)));

			browser.stop();
		}
	}
}
