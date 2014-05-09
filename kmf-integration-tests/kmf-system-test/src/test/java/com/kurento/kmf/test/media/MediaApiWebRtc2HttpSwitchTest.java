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

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.WebRtcChannel;

/**
 * <strong>Description</strong>: WebRTC to HTTP switch. Test KMS is able to
 * dynamically switch many WebRTC flows to a single HTTP endpoint Setup. Two
 * clients WebRTC send-only with audio/video: A,B. One HTTP-EP: H. Switch
 * between following scenarios: A to H, B to H. At least two round.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (A)</li>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (B)</li>
 * <li>WebRtcEndpoint (A) -> HttpGetEndpoint</li>
 * <li>WebRtcEndpoint (B) -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browsers starts before 60 seconds (default timeout)</li>
 * <li>Color received by HttpPlayer should be green (RGB #008700, video test of
 * Chrome)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiWebRtc2HttpSwitchTest extends MediaApiTest {

	@Test
	public void testWebRtc2HttpSwitch() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEndpoint1 = mp.newWebRtcEndpoint().build();
		WebRtcEndpoint webRtcEndpoint2 = mp.newWebRtcEndpoint().build();
		HttpGetEndpoint httpGetEndpoint = mp.newHttpGetEndpoint().build();

		webRtcEndpoint1.connect(webRtcEndpoint1);
		webRtcEndpoint2.connect(webRtcEndpoint2);

		try (BrowserClient browser1 = new BrowserClient(getServerPort(),
				Browser.CHROME_FOR_TEST, Client.WEBRTC);
				BrowserClient browser2 = new BrowserClient(getServerPort(),
						Browser.CHROME_FOR_TEST, Client.WEBRTC);
				BrowserClient browser3 = new BrowserClient(getServerPort(),
						Browser.CHROME_FOR_TEST, Client.PLAYER);) {

			// WebRTC
			browser1.subscribeEvents("playing");
			browser1.connectToWebRtcEndpoint(webRtcEndpoint1,
					WebRtcChannel.AUDIO_AND_VIDEO);
			browser2.subscribeEvents("playing");
			browser2.connectToWebRtcEndpoint(webRtcEndpoint2,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Wait until event playing in the remote stream
			Assert.assertTrue("Timeout waiting playing event",
					browser1.waitForEvent("playing"));
			Assert.assertTrue("Timeout waiting playing event",
					browser2.waitForEvent("playing"));

			// Round #1: Connecting WebRTC #1 to HttpEnpoint
			webRtcEndpoint1.connect(httpGetEndpoint);
			browser3.setURL(httpGetEndpoint.getUrl());
			browser3.subscribeEvents("playing");
			browser3.start();
			Assert.assertTrue("Timeout waiting playing event",
					browser3.waitForEvent("playing"));
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					browser3.colorSimilarTo(new Color(0, 135, 0)));

			// Guard time to see stream from WebRTC #1
			Thread.sleep(5000);

			// Round #2: Connecting WebRTC #2 to HttpEnpoint
			webRtcEndpoint2.connect(httpGetEndpoint);

			// Guard time to see stream from WebRTC #2
			Thread.sleep(5000);
			Assert.assertTrue(
					"The color of the video should be green (RGB #008700)",
					browser3.colorSimilarTo(new Color(0, 135, 0)));
		}
	}
}
