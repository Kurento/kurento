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
package org.kurento.test.base;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Before;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.latency.VideoTagType;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base for tests using kurento-client and Http Server.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@EnableAutoConfiguration
public class BrowserKurentoClientTest extends KurentoClientTest {

	public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);

	@Before
	public void setupHttpServer() throws Exception {
		if (!this.getClass().isAnnotationPresent(WebAppConfiguration.class)) {
			KurentoServicesTestHelper
					.startHttpServer(BrowserKurentoClientTest.class);
		}
	}

	protected void playRecording(Browser browserType, String recordingFile,
			int playtime, int x, int y, Color... expectedColors)
			throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).local().build()) {
			browser.subscribeEvents("playing", "ended");
			browser.playUrlInVideoTag(recordingFile, VideoTagType.REMOTE);

			// Assertions
			Assert.assertTrue(
					"Not received media in the recording (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			for (Color color : expectedColors) {
				Assert.assertTrue("The color of the recorded video should be "
						+ color, browser.similarColorAt(color, x, y));
			}
			Assert.assertTrue(
					"Not received end of the recording (timeout waiting ended event)",
					browser.waitForEvent("ended"));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue(
					"Error in play time in the recorded video (expected: "
							+ playtime + " sec, real: " + currentTime + " sec)",
					compare(playtime, currentTime));
		}
	}

	protected void playRecording(Browser browserType, String recordingFile,
			int playtime, Color... expectedColors) throws InterruptedException {
		playRecording(browserType, recordingFile, playtime, 0, 0,
				expectedColors);
	}
}
