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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
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
	private static final int TIMEOUT_EOS = 60; // seconds

	@Before
	public void setupHttpServer() throws Exception {
		if (!this.getClass().isAnnotationPresent(WebAppConfiguration.class)) {
			KurentoServicesTestHelper
					.startHttpServer(BrowserKurentoClientTest.class);
		}
	}

	protected void playFileWithPipeline(Browser browserType,
			String recordingFile, int playtime, int x, int y,
			Color... expectedColors) throws InterruptedException {

		MediaPipeline mp = null;
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).build()) {
			// Media Pipeline
			mp = kurentoClient.createMediaPipeline();
			PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
					recordingFile).build();
			WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
			playerEP.connect(webRtcEP);

			// Play latch
			final CountDownLatch eosLatch = new CountDownLatch(1);
			playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
				@Override
				public void onEvent(EndOfStreamEvent event) {
					eosLatch.countDown();
				}
			});

			// Test execution
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);
			playerEP.play();

			// Assertions
			makeAssertions("[played file with media pipeline]", browser,
					playtime, x, y, eosLatch, expectedColors);

		} finally {
			// Release Media Pipeline
			if (mp != null) {
				mp.release();
			}
		}
	}

	protected void playFileAsLocal(Browser browserType, String recordingFile,
			int playtime, int x, int y, Color... expectedColors)
			throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).client(Client.WEBRTC).local().build()) {
			browser.subscribeEvents("playing");
			browser.playUrlInVideoTag(recordingFile, VideoTagType.REMOTE);

			// Assertions
			makeAssertions("[played as local file]", browser, playtime, x, y,
					null, expectedColors);
		}
	}

	protected void playFileAsLocal(Browser browserType, String recordingFile,
			int playtime, Color... expectedColors) throws InterruptedException {
		playFileAsLocal(browserType, recordingFile, playtime, 0, 0,
				expectedColors);
	}

	protected void playFileWithPipeline(Browser browserType,
			String recordingFile, int playtime, Color... expectedColors)
			throws InterruptedException {
		playFileWithPipeline(browserType, recordingFile, playtime, 0, 0,
				expectedColors);
	}

	private void makeAssertions(String messageAppend, BrowserClient browser,
			int playtime, int x, int y, CountDownLatch eosLatch,
			Color... expectedColors) throws InterruptedException {
		Assert.assertTrue(
				"Not received media in the recording (timeout waiting playing event) "
						+ messageAppend, browser.waitForEvent("playing"));
		for (Color color : expectedColors) {
			Assert.assertTrue("The color of the recorded video should be "
					+ color + " " + messageAppend,
					browser.similarColorAt(color, x, y));
		}

		if (eosLatch != null) {
			Assert.assertTrue("Not received EOS event in player",
					eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));
		} else {
			Thread.sleep(playtime * 1000);
		}

		double currentTime = browser.getCurrentTime();
		Assert.assertTrue(
				"Error in play time in the recorded video (expected: "
						+ playtime + " sec, real: " + currentTime + " sec) "
						+ messageAppend, compare(playtime, currentTime));
	}
}
