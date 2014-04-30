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
package com.kurento.kmf.test.content;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.test.base.ContentApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a HTTP Player, using a HttpPlayerHandler in ther server-side.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class ContentApiPlayerTest extends ContentApiTest {

	private static final String HANDLER = "/player";

	@HttpPlayerService(path = HANDLER, redirect = false, useControlProtocol = false)
	public static class PlayerRedirect extends HttpPlayerHandler {

		private PlayerEndpoint playerEP;

		@Override
		public void onContentRequest(HttpPlayerSession session)
				throws Exception {
			MediaPipeline mp = session.getMediaPipelineFactory().create();
			playerEP = mp.newPlayerEndpoint(
					"http://ci.kurento.com/video/color/red.webm").build();
			HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
					.build();
			playerEP.connect(httpEP);
			session.start(httpEP);
		}

		@Override
		public void onContentStarted(HttpPlayerSession contentSession)
				throws Exception {
			playerEP.play();
		}
	}

	@Test
	public void testPlayer() throws InterruptedException {
		try (BrowserClient browser = new BrowserClient(getServerPort(),
				Browser.CHROME, Client.PLAYER)) {
			browser.setURL(HANDLER);
			browser.subscribeEvents("playing", "ended");
			browser.start();

			// Assertions
			Assert.assertTrue(browser.waitForEvent("playing"));
			Assert.assertTrue(browser.waitForEvent("ended"));
			Assert.assertTrue("Playback time must be at least 3 seconds",
					browser.getCurrentTime() >= 3);
			Assert.assertTrue(browser.colorSimilarTo(Color.RED));
		}
	}
}
