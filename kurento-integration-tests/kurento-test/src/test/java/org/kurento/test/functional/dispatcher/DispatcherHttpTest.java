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
package org.kurento.test.functional.dispatcher;

import org.kurento.test.base.FunctionalTest;
import org.kurento.test.config.TestScenario;

/**
 * 
 * <strong>Description</strong>: A Chrome browser opens a WebRtcEndpoint and
 * this stream is connected through a Dispatcher to an HttpGetEndpoint, played
 * in another browser.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> Dispatcher -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherHttpTest extends FunctionalTest {

	public DispatcherHttpTest(TestScenario testScenario) {
		super(testScenario);
	}

//	private static final int PLAYTIME = 5; // seconds to play in WebRTC
//	private static final int TIMEOUT = 120; // seconds
//
//	@Ignore
//	@Test
//	public void testDispatcherHttpChrome() throws Exception {
//		doTest(BrowserType.CHROME);
//	}
//
//	public void doTest(BrowserType browserType) throws Exception {
//		// Media Pipeline
//		MediaPipeline mp = kurentoClient.createMediaPipeline();
//		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();
//		HttpGetEndpoint httpEP = new HttpGetEndpoint.Builder(mp)
//				.terminateOnEOS().build();
//
//		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
//		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
//		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();
//
//		webRtcEP1.connect(hubPort1);
//		hubPort2.connect(httpEP);
//
//		dispatcher.connect(hubPort1, hubPort2);
//
//		// Test execution
//		try (BrowserClient browser1 = new BrowserClient.Builder()
//				.browserType(browserType).client(Client.WEBRTC).build();
//				BrowserClient browser2 = new BrowserClient.Builder()
//						.browserType(browserType).client(Client.PLAYER).build();) {
//
//			browser1.initWebRtc(webRtcEP1, WebRtcChannel.AUDIO_AND_VIDEO,
//					WebRtcMode.SEND_ONLY);
//
//			browser2.setTimeout(TIMEOUT);
//			browser2.setURL(httpEP.getUrl());
//			browser2.subscribeEvents("playing");
//			browser2.start();
//
//			// Assertions
//			Assert.assertTrue(
//					"Not received media (timeout waiting playing event)",
//					browser2.waitForEvent("playing"));
//			Assert.assertTrue(
//					"The color of the video should be green (RGB #008700)",
//					browser2.similarColor(CHROME_VIDEOTEST_COLOR));
//
//			Thread.sleep(PLAYTIME * 1000);
//		}
//
//		// Release Media Pipeline
//		mp.release();
//	}
}
