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
 * <strong>Description</strong>: Compatibility test for WebRTC in loopback.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Play time should be as expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
@RunWith(Parameterized.class)
public class WebRtcCompatibilityLoopbackTest extends CompatibilityTest {

	public WebRtcCompatibilityLoopbackTest(TestScenario testScenario) {
		super(testScenario);
	}

//	private static final int PLAYTIME = 10; // seconds
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
//				{ Platform.WIN8_1, BrowserType.CHROME, "39" } });
//	}
//
//	public WebRtcCompatibilityLoopbackTest(Platform platform,
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
//	public void testWebRtcCompatibilityLoopbackChrome()
//			throws InterruptedException {
//		// Media Pipeline
//		MediaPipeline mp = kurentoClient.createMediaPipeline();
//		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
//		webRtcEndpoint.connect(webRtcEndpoint);
//
//		// Browser
//		try (BrowserClient browser = new BrowserClient.Builder()
//				.browserType(browserType).browserVersion(browserVersion)
//				.platform(platform).client(Client.WEBRTC).build()) {
//			browser.subscribeEvents("playing");
//			browser.initWebRtc(webRtcEndpoint, WebRtcChannel.AUDIO_AND_VIDEO,
//					WebRtcMode.SEND_RCV);
//
//			// Guard time to play the video
//			Thread.sleep(PLAYTIME * 1000);
//
//			// Assertions
//			Assert.assertTrue(
//					"Not received media (timeout waiting playing event)",
//					browser.waitForEvent("playing"));
//			Assert.assertTrue(
//					"The color of the video should be green (RGB #008700)",
//					browser.similarColor(CHROME_VIDEOTEST_COLOR));
//			double currentTime = browser.getCurrentTime();
//			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
//					+ " sec, real: " + currentTime + " sec)",
//					compare(PLAYTIME, currentTime));
//		} finally {
//			// Release Media Pipeline
//			mp.release();
//		}
//	}
}
