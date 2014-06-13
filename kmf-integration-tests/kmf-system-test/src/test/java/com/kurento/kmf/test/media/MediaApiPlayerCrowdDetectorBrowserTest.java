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

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.*;
import com.kurento.kmf.media.events.*;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.*;

/**
 * Test of a HTTP Player with CrowdDetector Filter.
 * 
 * <strong>Description</strong>: HTTP Player with CrowdDetector Filter.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> CrowdDetectorFilter -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before 60 seconds (default timeout)</li>
 * <li>Occupancy events received</li>
 * <li>Fluidity event received</li>
 * <li>EOS event received</li>
 * <li>Browser ends before 60 seconds (default timeout)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 4.2.3
 */
public class MediaApiPlayerCrowdDetectorBrowserTest extends BrowserMediaApiTest {

	@Test
	public void testPlayerCrowdDetector() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/crowd.mp4").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();

		List<RegionOfInterest> rois = newArrayList();
		List<RelativePoint> points = new ArrayList<RelativePoint>();

		points.add(new RelativePoint(0, 0));
		points.add(new RelativePoint((float) 0.5, 0));
		points.add(new RelativePoint((float) 0.5, (float) 0.5));
		points.add(new RelativePoint(0, (float) 0.5));

		RegionOfInterestConfig config = new RegionOfInterestConfig();

		config.setFluidityLevelMin(10);
		config.setFluidityLevelMed(35);
		config.setFluidityLevelMax(65);
		config.setFluidityNumFramesToEvent(5);
		config.setOccupancyLevelMin(10);
		config.setOccupancyLevelMed(35);
		config.setOccupancyLevelMax(65);
		config.setOccupancyNumFramesToEvent(5);
		config.setSendOpticalFlowEvent(false);
		config.setOpticalFlowNumFramesToEvent(3);
		config.setOpticalFlowNumFramesToReset(3);
		config.setOpticalFlowAngleOffset(0);

		rois.add(new RegionOfInterest(points, config, "roi0"));

		CrowdDetectorFilter crowdDetectorFilter = mp.newCrowdDetectorFilter(
				rois).build();
		playerEP.connect(crowdDetectorFilter);
		crowdDetectorFilter.connect(httpEP);

		final List<EndOfStreamEvent> eosEvents = new ArrayList<>();
		playerEP.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosEvents.add(event);
			}
		});

		final List<CrowdDetectorOccupancyEvent> crowdDetectedOccupancyEvents = new ArrayList<>();
		final List<CrowdDetectorFluidityEvent> crowdDetectedFluidityEvents = new ArrayList<>();

		crowdDetectorFilter
				.addCrowdDetectorOccupancyListener(new MediaEventListener<CrowdDetectorOccupancyEvent>() {
					@Override
					public void onEvent(CrowdDetectorOccupancyEvent event) {
						crowdDetectedOccupancyEvents.add(event);
					}
				});

		crowdDetectorFilter
				.addCrowdDetectorFluidityListener(new MediaEventListener<CrowdDetectorFluidityEvent>() {
					@Override
					public void onEvent(CrowdDetectorFluidityEvent event) {
						crowdDetectedFluidityEvents.add(event);
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
			Assert.assertTrue("Play time must be at least 9 seconds",
					browser.getCurrentTime() > 9);
			Assert.assertFalse(
					"No occupancy events throw by crowd detector filter",
					crowdDetectedOccupancyEvents.isEmpty());
			Assert.assertFalse(
					"No fluidity events throw by crowd detector filter",
					crowdDetectedFluidityEvents.isEmpty());
			Assert.assertFalse("No EOS event", eosEvents.isEmpty());
		}
	}

}
