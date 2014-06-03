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
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.HubPort;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * 
 * <strong>Description</strong>: Test of a Composite Mixer.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>4xPlayerEndpoint -> Composite -> HttpGetEndpoint</li>
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
public class MediaApiCompositePlayerTest extends BrowserMediaApiTest {

	@Ignore
	@Test
	public void testCompositePlayerChrome() throws Exception {
		doTest(Browser.CHROME);
	}

	@Ignore
	@Test
	public void testCompositePlayerFirefox() throws Exception {
		doTest(Browser.FIREFOX);
	}

	public void doTest(Browser browserType) throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerRed = mp.newPlayerEndpoint(
				"file://" + getPathTestFiles() + "/video/60sec/red.webm")
				.build();
		PlayerEndpoint playerGreen = mp.newPlayerEndpoint(
				"file://" + getPathTestFiles() + "/video/60sec/green.webm")
				.build();
		PlayerEndpoint playerBlue = mp.newPlayerEndpoint(
				"file://" + getPathTestFiles() + "/video/60sec/blue.webm")
				.build();
		PlayerEndpoint playerWhite = mp.newPlayerEndpoint(
				"file://" + getPathTestFiles() + "/video/60sec/white.webm")
				.build();

		Composite composite = mp.newComposite().build();
		HubPort hubPort1 = composite.newHubPort().build();
		HubPort hubPort2 = composite.newHubPort().build();
		HubPort hubPort3 = composite.newHubPort().build();
		HubPort hubPort4 = composite.newHubPort().build();
		HubPort hubPort5 = composite.newHubPort().build();

		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerRed.connect(hubPort1);
		playerGreen.connect(hubPort2);
		playerBlue.connect(hubPort3);
		playerWhite.connect(hubPort4);

		hubPort5.connect(httpEP);

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing");
			playerRed.play();
			playerGreen.play();
			playerBlue.play();
			playerWhite.play();
			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Assert.assertTrue("Upper left part of the video must be red",
					browser.color(Color.RED, 10, 0, 0));
			Assert.assertTrue("Upper right part of the video must be green",
					browser.color(Color.GREEN, 11, 450, 0));
			Assert.assertTrue("Lower left part of the video must be blue",
					browser.color(Color.BLUE, 12, 0, 450));
			Assert.assertTrue("Lower right part of the video must be white",
					browser.color(Color.WHITE, 13, 450, 450));
		}
	}

}
