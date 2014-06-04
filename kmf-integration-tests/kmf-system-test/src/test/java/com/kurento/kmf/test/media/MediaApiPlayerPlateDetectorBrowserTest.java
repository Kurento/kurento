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
import org.junit.Ignore;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * <strong>Description</strong>: HTTP Player with PlateDetector Filter.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> PlateDetector -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before 60 seconds (default timeout)</li>
 * <li>Plates detected (2651DLC and 3882GKP)</li>
 * <li>EOS event received</li>
 * <li>Browser ends before 60 seconds (default timeout)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 4.2.3
 */
public class MediaApiPlayerPlateDetectorBrowserTest extends BrowserMediaApiTest {

	@Ignore
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
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			browser.start();

			// Assertions
			Assert.assertTrue(browser.waitForEvent("playing"));
			Assert.assertTrue(browser.waitForEvent("ended"));
			Assert.assertTrue("Play time must be at least 12 seconds",
					browser.getCurrentTime() > 12);
			Assert.assertTrue(
					"No plate 2651DLC detected by palte detector filter",
					platesDetectedEvents.contains("--2651DCL"));
			Assert.assertTrue(
					"No plate 3882GKP detected by palte detector filter",
					platesDetectedEvents.contains("--3882GKP"));
			Assert.assertFalse("No EOS event", eosEvents.isEmpty());
		}
	}

}
