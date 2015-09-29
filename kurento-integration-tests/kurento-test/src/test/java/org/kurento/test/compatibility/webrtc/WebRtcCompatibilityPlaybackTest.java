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
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.CompatibilityTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.TestScenario;
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
public class WebRtcCompatibilityPlaybackTest extends CompatibilityTest {

	private static final int PLAYTIME = 10; // seconds
	private static final String VIDEO_URL = "http://files.kurento.org/video/10sec/red.webm";
	private static final Color[] EXPECTED_COLORS = { Color.RED };

	public WebRtcCompatibilityPlaybackTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		// Test: Browsers in saucelabs
		TestScenario test1 = new TestScenario();
		test1.addBrowser(
				BrowserConfig.BROWSER,
				new Browser.Builder().webPageType(WebPageType.WEBRTC)
						.browserType(BrowserType.CHROME)
						.scope(BrowserScope.SAUCELABS)
						.platform(Platform.WIN8_1).browserVersion("39").build());

		TestScenario test2 = new TestScenario();
		test2.addBrowser(
				BrowserConfig.BROWSER,
				new Browser.Builder().webPageType(WebPageType.WEBRTC)
						.browserType(BrowserType.FIREFOX)
						.scope(BrowserScope.SAUCELABS).platform(Platform.LINUX)
						.browserVersion("35").build());

		return Arrays.asList(new Object[][] { { test1 }, { test2 } });
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
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
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
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));
		for (Color color : EXPECTED_COLORS) {
			Assert.assertTrue("The color of the video should be " + color,
					getPage().similarColor(color));
		}
		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
		double currentTime = getPage().getCurrentTime();
		Assert.assertTrue("Error in play time (expected: " + PLAYTIME
				+ " sec, real: " + currentTime + " sec)",
				getPage().compare(PLAYTIME, currentTime));

		// Release Media Pipeline
		mp.release();
	}
}
