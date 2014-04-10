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
package com.kurento.kmf.test.media;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.EventListener;
import com.kurento.kmf.test.client.VideoTagBrowser;

/**
 * Test of a HTTP Player, using directly a MediaPipeline and Selenium.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiPlayerBrowserTest extends MediaApiTest {

	@Test
	public void testPlayer() throws Exception {
		// Media Pipeline and Media Elements
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/small.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP.connect(httpEP);
		String url = httpEP.getUrl();
		log.info("url: {}", url);

		// Test execution
		final CountDownLatch startEvent = new CountDownLatch(1);
		final CountDownLatch terminationEvent = new CountDownLatch(1);

		try (VideoTagBrowser vtb = new VideoTagBrowser(getServerPort(),
				Browser.CHROME)) {
			vtb.setURL(url);
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
			playerEP.play();
			vtb.start();

			Assert.assertTrue(startEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long startTime = System.currentTimeMillis();
			Assert.assertTrue(terminationEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long duration = System.currentTimeMillis() - startTime;
			log.info("Video duration: " + (duration / 60) + " seconds");
		}
	}

}
