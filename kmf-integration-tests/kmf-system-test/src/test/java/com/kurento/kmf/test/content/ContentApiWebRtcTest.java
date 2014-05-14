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
package com.kurento.kmf.test.content;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.ContentApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a WebRTC in loopback.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class ContentApiWebRtcTest extends ContentApiTest {

	private static final String HANDLER = "/webrtc";

	@WebRtcContentService(path = HANDLER)
	public static class WebRtcHandler extends WebRtcContentHandler {

		@Override
		public void onContentRequest(WebRtcContentSession session)
				throws Exception {
			MediaPipeline mp = session.getMediaPipelineFactory().create();
			session.releaseOnTerminate(mp);
			WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
			webRtcEndpoint.connect(webRtcEndpoint);
			session.start(webRtcEndpoint);

			terminateLatch = new CountDownLatch(1);
		}

		@Override
		public void onSessionTerminated(WebRtcContentSession session, int code,
				String reason) throws Exception {
			super.onSessionTerminated(session, code, reason);
			terminateLatch.countDown();
		}

	}

	@Test
	public void testWebRtc() throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.FIREFOX).client(Client.WEBRTC).build()) {

			browser.setURL(HANDLER);
			browser.subscribeEvents("playing");
			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// Guard time to see the loopback
			Thread.sleep(5000);

			// Ending session in order
			browser.stop();
			Assert.assertTrue(
					"Timeout waiting onSessionTerminated",
					terminateLatch.await(browser.getTimeout(), TimeUnit.SECONDS));
		}
	}
}
