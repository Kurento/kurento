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
package org.kurento.test.client;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.test.base.BrowserKurentoClientTest;

/**
 * <strong>Description</strong>: HTTP Player switching videos.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>5xPlayerEndpoint -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Play time should be the expected</li>
 * <li>Browser ends before default timeout</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class PlayerSwitchBrowserTest extends BrowserKurentoClientTest {

	@Test
	public void testPlayerSwitch() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/red.webm").build();
		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/green.webm").build();
		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/blue.webm").build();
		PlayerEndpoint playerSmpte = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/smpte.webm").build();
		PlayerEndpoint playerBall = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/ball.webm").build();
		HttpGetEndpoint httpEP = new HttpGetEndpoint.Builder(mp)
				.terminateOnEOS().build();

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.PLAYER).build()) {
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
			Assert.assertTrue("Play time must be at least 8 seconds",
					browser.getCurrentTime() >= 8);
		}
	}

}