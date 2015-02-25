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

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kurento.test.base.CompatibilityTest;
import org.kurento.test.config.TestScenario;

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

	public WebRtcCompatibilityPlaybackTest(TestScenario testScenario) {
		super(testScenario);
	}
	
//	private static final int PLAYTIME = 10; // seconds
//	private static final int TIMEOUT_EOS = 60; // seconds
//	private static final String VIDEO_URL = "http://files.kurento.org/video/10sec/red.webm";
//	private static final Color[] EXPECTED_COLORS = { Color.RED };
//
//	private Platform platform;
//	private BrowserType browserType;
//	private String browserVersion;
//
//	@Parameters(name = "Platform={0}, Browser={1}, BrowserVersion={2}")
//	public static Collection<Object[]> data() {
//		return Arrays.asList(new Object[][] {
//				{ Platform.WIN8_1, BrowserType.CHROME, "28" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "29" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "30" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "31" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "32" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "33" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "34" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "35" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "36" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "37" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "38" },
//				{ Platform.WIN8_1, BrowserType.CHROME, "39" },
//
//				{ Platform.LINUX, BrowserType.FIREFOX, "23" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "24" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "25" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "26" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "27" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "28" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "29" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "30" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "31" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "32" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "33" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "34" },
//				{ Platform.LINUX, BrowserType.FIREFOX, "35" } });
//	}
//
//	public WebRtcCompatibilityPlaybackTest(Platform platform,
//			BrowserType browserType, String browserVersion) {
//		this.platform = platform;
//		this.browserType = browserType;
//		this.browserVersion = browserVersion;
//
//		log.debug(
//				"Starting test with the following parameters: platform={}, browserType={}, browserVersion={}",
//				this.platform, this.browserType, this.browserVersion);
//	}
//
//	@Test
//	public void testWebRtcCompatibilityPlayback() throws InterruptedException {
//		// Media Pipeline
//		MediaPipeline mp = kurentoClient.createMediaPipeline();
//		PlayerEndpoint playerEndpoint = new PlayerEndpoint.Builder(mp,
//				VIDEO_URL).build();
//		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
//		playerEndpoint.connect(webRtcEndpoint);
//
//		// Browser
//		try (BrowserClient browser = new BrowserClient.Builder()
//				.browserType(browserType).browserVersion(browserVersion)
//				.platform(platform).client(Client.WEBRTC).build()) {
//			browser.subscribeEvents("playing");
//			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
//					WebRtcMode.RCV_ONLY);
//			playerEndpoint.play();
//
//			// Subscription to EOS event
//			final CountDownLatch eosLatch = new CountDownLatch(1);
//			playerEndpoint
//					.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
//						@Override
//						public void onEvent(EndOfStreamEvent event) {
//							eosLatch.countDown();
//						}
//					});
//
//			// Assertions
//			Assert.assertTrue(
//					"Not received media (timeout waiting playing event)",
//					browser.waitForEvent("playing"));
//			for (Color color : EXPECTED_COLORS) {
//				Assert.assertTrue("The color of the video should be " + color,
//						browser.similarColor(color));
//			}
//			Assert.assertTrue("Not received EOS event in player",
//					eosLatch.await(TIMEOUT_EOS, TimeUnit.SECONDS));
//			double currentTime = browser.getCurrentTime();
//			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
//					+ " sec, real: " + currentTime + " sec)",
//					compare(PLAYTIME, currentTime));
//
//		} finally {
//			// Release Media Pipeline
//			mp.release();
//		}
//	}
}
