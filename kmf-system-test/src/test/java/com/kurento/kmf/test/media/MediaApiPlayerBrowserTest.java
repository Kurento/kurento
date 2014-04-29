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
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a HTTP Player, using directly a MediaPipeline and Selenium.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiPlayerBrowserTest extends MediaApiTest {

	@Test
	public void testPlayer() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/color/blue.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP.connect(httpEP);

		// Test execution
		try (BrowserClient browser = new BrowserClient(getServerPort(),
				Browser.CHROME, Client.PLAYER)) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			browser.start();

			// Assertions
			Assert.assertTrue(browser.waitForEvent("playing"));
			Assert.assertTrue(browser.waitForEvent("ended"));
			Assert.assertTrue("Playback time must be at least 3 seconds",
					browser.getCurrentTime() >= 3);
			Assert.assertTrue(browser.colorSimilarTo(Color.BLUE));
		}
	}

}
