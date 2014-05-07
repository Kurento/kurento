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
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a HTTP Player with PlateDetector Filter.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 4.2.3
 */
public class MediaApiPlayerPlateDetectorBrowserTest extends MediaApiTest {

	@Test
	public void testPlayerPlateDetector() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/plates.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		PlateDetectorFilter plateDetectorFilter = mp.newPlateDetectorFilter()
				.build();
		playerEP.connect(plateDetectorFilter);
		plateDetectorFilter.connect(httpEP);
		plateDetectorFilter.setPlateWidthPercentage((float) 0.3);

		final List<EndOfStreamEvent> eosEvents = new ArrayList<>();
		playerEP.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosEvents.add(event);
			}
		});

		final List<String> platesDetectedEvents = new ArrayList<>();
		plateDetectorFilter
				.addPlateDetectedListener(new MediaEventListener<PlateDetectedEvent>() {
					@Override
					public void onEvent(PlateDetectedEvent event) {
						log.info("Plate Detected {}", event.getPlate());
						platesDetectedEvents.add(event.getPlate());
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
			Assert.assertTrue(
					"No plate 2651DLC detected by palte detector filter",
					platesDetectedEvents.contains("--2651DCL"));
			Assert.assertTrue(
					"No plate 3882GKP detected by palte detector filter",
					platesDetectedEvents.contains("--3882GKP"));
			Assert.assertFalse("No EOS event", eosEvents.isEmpty());
			Assert.assertTrue("Playback time must be at least 12 seconds",
					browser.getCurrentTime() > 12);
		}
	}

}
