/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.compatibility.webrtc;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.CompatibilityTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.openqa.selenium.Platform;

/**
 * <strong>Description</strong>: Compatibility test for WebRTC in playback.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Color of the video should be the expected (x3: red, green, blue)</li>
 * <li>End of stream event should be received in PlayerEndpoint</li>
 * <li>Play time should be as expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
@RunWith(Parameterized.class)
public class WebRtcCompatibilityPlaybackTest extends CompatibilityTest {

	private static final int PLAYTIME = 10; // seconds
	private static final int TIMEOUT_EOS = 60; // seconds
	private static final String VIDEO_URL = "http://files.kurento.org/video/10sec/red.webm";
	private static final Color[] EXPECTED_COLORS = { Color.RED };

	private Platform platform;
	private Browser browserType;
	private String browserVersion;

	@Parameters(name = "Platform={0}, Browser={1}, BrowserVersion={2}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ Platform.WIN8_1, Browser.CHROME, "28" },
				{ Platform.WIN8_1, Browser.CHROME, "29" },
				{ Platform.WIN8_1, Browser.CHROME, "30" },
				{ Platform.WIN8_1, Browser.CHROME, "31" },
				{ Platform.WIN8_1, Browser.CHROME, "32" },
				{ Platform.WIN8_1, Browser.CHROME, "33" },
				{ Platform.WIN8_1, Browser.CHROME, "34" },
				{ Platform.WIN8_1, Browser.CHROME, "35" },
				{ Platform.WIN8_1, Browser.CHROME, "36" },
				{ Platform.WIN8_1, Browser.CHROME, "37" },
				{ Platform.WIN8_1, Browser.CHROME, "38" },
				{ Platform.WIN8_1, Browser.CHROME, "39" },

				{ Platform.LINUX, Browser.FIREFOX, "23" },
				{ Platform.LINUX, Browser.FIREFOX, "24" },
				{ Platform.LINUX, Browser.FIREFOX, "25" },
				{ Platform.LINUX, Browser.FIREFOX, "26" },
				{ Platform.LINUX, Browser.FIREFOX, "27" },
				{ Platform.LINUX, Browser.FIREFOX, "28" },
				{ Platform.LINUX, Browser.FIREFOX, "29" },
				{ Platform.LINUX, Browser.FIREFOX, "30" },
				{ Platform.LINUX, Browser.FIREFOX, "31" },
				{ Platform.LINUX, Browser.FIREFOX, "32" },
				{ Platform.LINUX, Browser.FIREFOX, "33" },
				{ Platform.LINUX, Browser.FIREFOX, "34" },
				{ Platform.LINUX, Browser.FIREFOX, "35" } });
	}

	public WebRtcCompatibilityPlaybackTest(Platform platform,
			Browser browserType, String browserVersion) {
		this.platform = platform;
		this.browserType = browserType;
		this.browserVersion = browserVersion;

		log.debug(
				"Starting test with the following parameters: platform={}, browserType={}, browserVersion={}",
				this.platform, this.browserType, this.browserVersion);
	}

	@Test
	public void testWebRtcCompatibilityPlayback() throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEndpoint = new PlayerEndpoint.Builder(mp,
				VIDEO_URL).build();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
		playerEndpoint.connect(webRtcEndpoint);

		// Browser
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(browserType).browserVersion(browserVersion)
				.platform(platform).client(Client.WEBRTC).build()) {
			browser.subscribeEvents("playing");
			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);
			playerEndpoint.play();

			// Subscription to EOS event
			final CountDownLatch eosLatch = new CountDownLatch(1);
			playerEndpoint
					.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
						@Override
						public void onEvent(EndOfStreamEvent event) {
							eosLatch.countDown();
						}
					});

			// Assertions
			Assert.assertTrue(
					"Not received media (timeout waiting playing event)",
					browser.waitForEvent("playing"));
			for (Color color : EXPECTED_COLORS) {
				Assert.assertTrue("The color of the video should be " + color,
						browser.similarColor(color));
			}
			Assert.assertTrue("Not received EOS event in player",
					eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
					+ " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));

		} finally {
			// Release Media Pipeline
			mp.release();
		}
	}
}
