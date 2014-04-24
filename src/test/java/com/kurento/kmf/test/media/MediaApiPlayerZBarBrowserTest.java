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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a HTTP Player with ZBar Filter.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class MediaApiPlayerZBarBrowserTest extends MediaApiTest {

	@Test
	public void testPlayerZBar() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/barcodes.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		ZBarFilter zBarFilter = mp.newZBarFilter().build();
		playerEP.connect(zBarFilter);
		zBarFilter.connect(httpEP);

		final List<EndOfStreamEvent> eosEvents = new ArrayList<>();
		playerEP.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosEvents.add(event);
			}
		});

		final List<CodeFoundEvent> codeFoundEvents = new ArrayList<>();
		zBarFilter
				.addCodeFoundListener(new MediaEventListener<CodeFoundEvent>() {
					@Override
					public void onEvent(CodeFoundEvent event) {
						log.info("CodeFound {}", event.getValue());
						codeFoundEvents.add(event);
					}
				});

		// Test execution
		try (BrowserClient browser = new BrowserClient(getServerPort(),
				Browser.CHROME, Client.PLAYER)) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			browser.start();

			// Assertions
			Assert.assertTrue(browser.waitForEvent("playing"));
			Assert.assertTrue(browser.waitForEvent("ended"));
			Assert.assertFalse(codeFoundEvents.isEmpty());
			Assert.assertFalse(eosEvents.isEmpty());
			Assert.assertTrue("Playback time must be at least 12 seconds",
					browser.getCurrentTime() > 12);
		}
	}

}
