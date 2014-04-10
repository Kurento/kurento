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

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.test.base.ContentApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.EventListener;
import com.kurento.kmf.test.client.VideoTagBrowser;

/**
 * Test of a HTTP Player, using a HttpPlayerHandler in ther server-side.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class ContentApiPlayerTest extends ContentApiTest {

	private static final String HANDLER = "/player";

	@HttpPlayerService(path = HANDLER, redirect = true, useControlProtocol = false)
	public static class PlayerRedirect extends HttpPlayerHandler {

		@Override
		public void onContentRequest(HttpPlayerSession session)
				throws Exception {
			MediaPipeline mp = session.getMediaPipelineFactory().create();
			PlayerEndpoint playerEP = mp.newPlayerEndpoint(
					"http://ci.kurento.com/video/small.webm").build();
			HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
					.build();
			playerEP.connect(httpEP);
			session.start(httpEP);
			session.setAttribute("player", playerEP);
		}

		@Override
		public void onContentStarted(HttpPlayerSession contentSession)
				throws Exception {
			if (contentSession.getAttribute("player") != null) {
				PlayerEndpoint playerEndpoint = (PlayerEndpoint) contentSession
						.getAttribute("player");

				playerEndpoint.play();
			}
		}
	}

	@Test
	public void testPlayer() throws InterruptedException {
		final CountDownLatch startEvent = new CountDownLatch(1);
		final CountDownLatch terminationEvent = new CountDownLatch(1);

		try (VideoTagBrowser vtb = new VideoTagBrowser(getServerPort(),
				Browser.CHROME)) {
			vtb.setURL(HANDLER);
			vtb.addEventListener("playing", new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("*** playing ***");
					startEvent.countDown();
				}
			});
			vtb.addEventListener("ended", new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("*** ended ***");
					terminationEvent.countDown();
				}
			});

			vtb.start();

			Assert.assertTrue(startEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long startTime = System.currentTimeMillis();

			Assert.assertTrue(terminationEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long duration = System.currentTimeMillis() - startTime;

			log.info("Video duration: " + (duration / 60) + " seconds");
		}
	}
}
