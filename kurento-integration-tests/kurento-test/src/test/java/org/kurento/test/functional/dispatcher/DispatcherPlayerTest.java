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
 * <strong>Description</strong>: A PlayerEndpoint is connected to a
 * WebRtcEndpoint through a Dispatcher.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> Dispatcher -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time should be the expected</li>
 * <li>Color of the video should be the expected</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherPlayerTest extends FunctionalTest {
	
	public DispatcherPlayerTest(TestScenario testScenario) {
		super(testScenario);
	}

//	private static final int PLAYTIME = 10; // seconds
//	private static final int TIMEOUT = 120; // seconds
//
//	@Test
//	public void testDispatcherPlayerChrome() throws Exception {
//		doTest(BrowserType.CHROME);
//	}
//
//	@Test
//	public void testDispatcherPlayerFirefox() throws Exception {
//		doTest(BrowserType.FIREFOX);
//	}
//
//	public void doTest(BrowserType browserType) throws Exception {
//		// Media Pipeline
//		MediaPipeline mp = kurentoClient.createMediaPipeline();
//
//		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
//				"http://files.kurento.org/video/10sec/red.webm").build();
//		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
//
//		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
//		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
//		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();
//
//		playerEP.connect(hubPort1);
//		hubPort2.connect(webRtcEP);
//		dispatcher.connect(hubPort1, hubPort2);
//
//		final CountDownLatch eosLatch = new CountDownLatch(1);
//		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
//			@Override
//			public void onEvent(EndOfStreamEvent event) {
//				eosLatch.countDown();
//			}
//		});
//
//		// Test execution
//		try (BrowserClient browser = new BrowserClient.Builder()
//				.browserType(browserType).client(Client.WEBRTC).build()) {
//			browser.setTimeout(TIMEOUT);
//			browser.subscribeEvents("playing");
//			browser.initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
//					WebRtcMode.RCV_ONLY);
//			playerEP.play();
//
//			// Assertions
//			Assert.assertTrue(
//					"Not received media (timeout waiting playing event)",
//					browser.waitForEvent("playing"));
//			Assert.assertTrue("The color of the video should be red",
//					browser.similarColor(Color.RED));
//			Assert.assertTrue("Not received EOS event in player",
//					eosLatch.await(TIMEOUT, TimeUnit.SECONDS));
//			double currentTime = browser.getCurrentTime();
//			Assert.assertTrue("Error in play time (expected: " + PLAYTIME
//					+ " sec, real: " + currentTime + " sec)",
//					compare(PLAYTIME, currentTime));
//		}
//
//		// Release Media Pipeline
//		mp.release();
//	}
}
