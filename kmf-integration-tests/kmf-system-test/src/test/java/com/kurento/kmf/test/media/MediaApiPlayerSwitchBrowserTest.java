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

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * <strong>Description</strong>: HTTP Player switching videos.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> (N) HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before 60 seconds (default timeout)</li>
 * <li>Play time should be the expected (at least 8 seconds)</li>
 * <li>Browser ends before 60 seconds (default timeout)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiPlayerSwitchBrowserTest extends MediaApiTest {

	@Test
	public void testPlayerSwitch() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerRed = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/gst/red.webm").build();
		PlayerEndpoint playerGreen = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/gst/green.webm").build();
		PlayerEndpoint playerBlue = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/gst/blue.webm").build();
		PlayerEndpoint playerSmpte = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/gst/smpte.webm").build();
		PlayerEndpoint playerBall = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/gst/ball.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME_FOR_TEST).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());

			// red
			playerRed.connect(httpEP);
			playerRed.play();
			browser.subscribeEvents("playing", "ended");
			browser.start();
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Thread.sleep(2000);

			// green
			playerGreen.connect(httpEP);
			playerGreen.play();
			Thread.sleep(2000);

			// blue
			playerBlue.connect(httpEP);
			playerBlue.play();
			Thread.sleep(2000);

			// smpte
			playerSmpte.connect(httpEP);
			playerSmpte.play();
			Thread.sleep(2000);

			// ball
			playerBall.connect(httpEP);
			playerBall.play();
			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			Assert.assertTrue("Playback time must be at least 8 seconds",
					browser.getCurrentTime() >= 8);
		}
	}

}