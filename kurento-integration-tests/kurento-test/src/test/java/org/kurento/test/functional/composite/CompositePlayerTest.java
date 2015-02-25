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
package org.kurento.test.functional.composite;

import org.kurento.test.base.FunctionalTest;
import org.kurento.test.config.TestScenario;

/**
 * 
 * <strong>Description</strong>: Four synthetic videos are played by four
 * PlayerEndpoint and mixed by a Composite. The resulting video is played in an
 * WebRtcEndpoint.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>4xPlayerEndpoint -> Composite -> WebRtcEndpoint</li>
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
public class CompositePlayerTest extends FunctionalTest {
	
	public CompositePlayerTest(TestScenario testScenario) {
		super(testScenario);
	}

//	private static int PLAYTIME = 5; // seconds
//
//	@Test
//	public void testCompositePlayerChrome() throws Exception {
//		doTest(BrowserType.CHROME);
//	}
//
//	@Test
//	public void testCompositePlayerFirefox() throws Exception {
//		doTest(BrowserType.FIREFOX);
//	}
//
//	public void doTest(BrowserType browserType) throws Exception {
//		// Media Pipeline
//		MediaPipeline mp = kurentoClient.createMediaPipeline();
//
//		PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/30sec/red.webm").build();
//		PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/30sec/green.webm").build();
//		PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/30sec/blue.webm").build();
//		PlayerEndpoint playerWhite = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/30sec/white.webm").build();
//
//		Composite composite = new Composite.Builder(mp).build();
//		HubPort hubPort1 = new HubPort.Builder(composite).build();
//		HubPort hubPort2 = new HubPort.Builder(composite).build();
//		HubPort hubPort3 = new HubPort.Builder(composite).build();
//		HubPort hubPort4 = new HubPort.Builder(composite).build();
//		HubPort hubPort5 = new HubPort.Builder(composite).build();
//		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
//
//		playerRed.connect(hubPort1);
//		playerGreen.connect(hubPort2);
//		playerBlue.connect(hubPort3);
//		playerWhite.connect(hubPort4);
//
//		hubPort5.connect(webRtcEP);
//
//		// Test execution
//		try (BrowserClient browser = new BrowserClient.Builder()
//				.browserType(browserType).client(Client.WEBRTC).build()) {
//			browser.subscribeEvents("playing");
//			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
//					WebRtcMode.RCV_ONLY);
//
//			playerRed.play();
//			playerGreen.play();
//			playerBlue.play();
//			playerWhite.play();
//
//			// Assertions
//			Assert.assertTrue(
//					"Not received media (timeout waiting playing event)",
//					browser.waitForEvent("playing"));
//			Assert.assertTrue("Upper left part of the video must be red",
//					browser.similarColorAt(Color.RED, 0, 0));
//			Assert.assertTrue("Upper right part of the video must be green",
//					browser.similarColorAt(Color.GREEN, 450, 0));
//			Assert.assertTrue("Lower left part of the video must be blue",
//					browser.similarColorAt(Color.BLUE, 0, 450));
//			Assert.assertTrue("Lower right part of the video must be white",
//					browser.similarColorAt(Color.WHITE, 450, 450));
//
//			// Guard time to see the composite result
//			Thread.sleep(PLAYTIME * 1000);
//		}
//
//		// Release Media Pipeline
//		mp.release();
//	}

}
