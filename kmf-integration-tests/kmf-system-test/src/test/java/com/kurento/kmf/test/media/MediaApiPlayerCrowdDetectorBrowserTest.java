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

import com.kurento.kmf.media.CrowdDetectorFilter;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.RegionOfInterest;
import com.kurento.kmf.media.RegionOfInterestConfig;
import com.kurento.kmf.media.RelativePoint;
import com.kurento.kmf.media.events.CrowdDetectorFluidityEvent;
import com.kurento.kmf.media.events.CrowdDetectorOccupancyEvent;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.test.base.MediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of a HTTP Player with CrowdDetector Filter.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 4.2.3
 */
public class MediaApiPlayerCrowdDetectorBrowserTest extends MediaApiTest {

	@Test
	public void testPlayerCrowdDetector() throws Exception {
		// Media Pipeline
		MediaPipeline mp = pipelineFactory.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/crowd.mp4").build();
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
		try (BrowserClient browser = new BrowserClient(getServerPort(),
				Browser.CHROME, Client.PLAYER)) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			browser.start();

			// Assertions
			Assert.assertTrue(browser.waitForEvent("playing"));
			Assert.assertTrue(browser.waitForEvent("ended"));
			Assert.assertFalse(
					"No occupancy events throw by crowd detector filter",
					crowdDetectedOccupancyEvents.isEmpty());
			Assert.assertFalse(
					"No fluidity events throw by crowd detector filter",
					crowdDetectedFluidityEvents.isEmpty());
			Assert.assertFalse("No EOS event", eosEvents.isEmpty());
			Assert.assertTrue("Playback time must be at least 10 seconds",
					browser.getCurrentTime() > 10);
		}
	}

}
