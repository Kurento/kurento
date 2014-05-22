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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.*;
import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.test.base.ContentApiTest;
import com.kurento.kmf.test.client.*;

/**
 * Test of a HTTP Player and ZBar filter.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class ContentApiPlayerZBarTest extends ContentApiTest {

	private static Logger log = LoggerFactory
			.getLogger(ContentApiPlayerZBarTest.class);

	private static final String HANDLER = "/playerZbar";

	@HttpPlayerService(path = HANDLER, redirect = true, useControlProtocol = true)
	public static class PlayerRedirect extends HttpPlayerHandler {

		private PlayerEndpoint playerEP;

		@Override
		public void onContentRequest(final HttpPlayerSession session)
				throws Exception {
			MediaPipeline mp = session.getMediaPipelineFactory().create();
			playerEP = mp.newPlayerEndpoint(
					"http://ci.kurento.com/video/barcodes.webm").build();
			HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
					.build();
			ZBarFilter zBarFilter = mp.newZBarFilter().build();
			playerEP.connect(zBarFilter);
			zBarFilter.connect(httpEP);
			session.start(httpEP);
			session.setAttribute("eventValue", "");
			zBarFilter
			.addCodeFoundListener(new MediaEventListener<CodeFoundEvent>() {
				@Override
				public void onEvent(CodeFoundEvent event) {
					log.info("Code Found " + event.getValue());
					if (session.getAttribute("eventValue").toString()
							.equals(event.getValue())) {
						return;
					}
					session.setAttribute("eventValue", event.getValue());
					session.publishEvent(new ContentEvent(event
							.getType(), event.getValue()));
				}
			});

			terminateLatch = new CountDownLatch(1);
		}

		@Override
		public void onContentStarted(HttpPlayerSession session)
				throws Exception {
			playerEP.play();
		}

		@Override
		public void onSessionTerminated(HttpPlayerSession session, int code,
				String reason) throws Exception {
			super.onSessionTerminated(session, code, reason);
			terminateLatch.countDown();
		}
	}

	@Test
	public void testPlayerZbar() throws InterruptedException {
		try (BrowserClient browser = new BrowserClient.Builder()
		.browser(Browser.CHROME).client(Client.PLAYERJSON).build()) {
			browser.setURL(HANDLER);
			browser.subscribeEvents("playing", "ended");
			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));

			// Ending session in order
			browser.stop();
			Assert.assertTrue(
					"Timeout waiting onSessionTerminated",
					terminateLatch.await(browser.getTimeout(), TimeUnit.SECONDS));
		}
	}
}
