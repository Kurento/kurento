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
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.EventListener;
import com.kurento.kmf.test.client.VideoTagBrowser;

/**
 * Test of a WebRTC in loopback.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class ContentApiWebRtcTst extends ContentApiTest {

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
		}

	}

	@Test
	public void testWebRtc() throws InterruptedException {
		final CountDownLatch startEvent = new CountDownLatch(1);

		try (VideoTagBrowser vtb = new VideoTagBrowser(getServerPort(),
				Browser.CHROME, Client.WEBRTC)) {
			vtb.setURL(HANDLER);
			vtb.addEventListener("playing", new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("*** playing ***");
					startEvent.countDown();
				}
			});
			vtb.start();

			Assert.assertTrue(startEvent.await(TIMEOUT, TimeUnit.SECONDS));

			// Guard time to see the remote video
			Thread.sleep(3000);
		}
	}
}
