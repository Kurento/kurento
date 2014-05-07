/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.demo.internal;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.demo.player.PlayerRedirect;
import com.kurento.demo.player.PlayerTunnel;
import com.kurento.demo.playerjson.PlayerJsonRedirect;
import com.kurento.demo.playerjson.PlayerJsonTunnel;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.CrowdDetectorFilter;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.PointerDetectorFilter;
import com.kurento.kmf.media.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.RegionOfInterest;
import com.kurento.kmf.media.RegionOfInterestConfig;
import com.kurento.kmf.media.RelativePoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.kmf.media.events.WindowInEvent;

/**
 * Static class which contains a generic implementation of an HTTP Player,
 * selecting the video to be played depending on the <code>contentId</code>.
 * This static code will be used in {@link PlayerRedirect}, {@link PlayerTunnel}
 * , {@link PlayerJsonRedirect} and {@link PlayerJsonTunnel}.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
public class GenericPlayer {

	public static final Log log = LogFactory.getLog(GenericPlayer.class);

	public static void play(final HttpPlayerSession session) {
		// contendId discriminates between a termination or a play. In case of
		// the play, contentId selects the URL and filter to be employed
		String contentId = session.getContentId();

		if (contentId != null && contentId.equalsIgnoreCase("reject")) {
			session.terminate(407, "Reject in player handler");
		} else {
			// Small video in WEBM by default (small.webm)
			String url = VideoURLs.map.get("small-webm");
			if (contentId != null && VideoURLs.map.containsKey(contentId)) {
				url = VideoURLs.map.get(contentId);
			}

			MediaPipelineFactory mpf = session.getMediaPipelineFactory();
			MediaPipeline mp = mpf.create();
			session.releaseOnTerminate(mp);
			PlayerEndpoint playerEndpoint = mp.newPlayerEndpoint(url).build();
			HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
					.build();
			if (contentId != null && contentId.equalsIgnoreCase("jack")) {
				// Jack Vader Filter
				JackVaderFilter filter = mp.newJackVaderFilter().build();
				playerEndpoint.connect(filter);
				filter.connect(httpEP);

			} else if (contentId != null && contentId.equalsIgnoreCase("zbar")) {
				// ZBar Filter
				ZBarFilter zBarFilter = mp.newZBarFilter().build();
				playerEndpoint.connect(zBarFilter);
				zBarFilter.connect(httpEP);
				session.setAttribute("eventValue", "");
				zBarFilter
						.addCodeFoundListener(new MediaEventListener<CodeFoundEvent>() {
							@Override
							public void onEvent(CodeFoundEvent event) {
								log.info("Code Found " + event.getValue());
								if (session.getAttribute("eventValue")
										.toString().equals(event.getValue())) {
									return;
								}
								session.setAttribute("eventValue",
										event.getValue());
								session.publishEvent(new ContentEvent(event
										.getType(), event.getValue()));
							}
						});
			}

			else if (contentId != null && contentId.equalsIgnoreCase("crowd")) {
				// Crowd Detector Filter
				List<RelativePoint> points = new ArrayList<RelativePoint>();
				points.add(new RelativePoint(0, 0));
				points.add(new RelativePoint(1, 0));
				points.add(new RelativePoint(1, 1));
				points.add(new RelativePoint(0, 1));
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

				List<RegionOfInterest> rois = newArrayList(new RegionOfInterest(
						points, config, "Roi"));
				CrowdDetectorFilter crowdDetector = mp.newCrowdDetectorFilter(
						rois).build();
				playerEndpoint.connect(crowdDetector);
				crowdDetector.connect(httpEP);
			}

			else if (contentId != null && contentId.equalsIgnoreCase("plate")) {
				// Plate Detector Filter
				PlateDetectorFilter plateDetectorFilter = mp
						.newPlateDetectorFilter().build();
				playerEndpoint.connect(plateDetectorFilter);
				plateDetectorFilter.connect(httpEP);
				session.setAttribute("plateValue", "");
				plateDetectorFilter
						.addPlateDetectedListener(new MediaEventListener<PlateDetectedEvent>() {
							@Override
							public void onEvent(PlateDetectedEvent event) {
								if (session.getAttribute("plateValue")
										.toString().equals(event.getPlate())) {
									return;
								}
								session.setAttribute("plateValue",
										event.getPlate());
								session.publishEvent(new ContentEvent(event
										.getType(), event.getPlate()));
							}
						});
			}

			else if (contentId != null && contentId.equalsIgnoreCase("pointer")) {
				// Pointer Detector Filter
				PointerDetectorFilter pointerDetectorFilter = mp
						.newPointerDetectorFilter().build();
				pointerDetectorFilter
						.addWindow(new PointerDetectorWindowMediaParam("goal",
								50, 50, 150, 150));
				pointerDetectorFilter
						.addWindowInListener(new MediaEventListener<WindowInEvent>() {
							@Override
							public void onEvent(WindowInEvent event) {
								session.publishEvent(new ContentEvent(event
										.getType(), event.getWindowId()));
							}
						});

				playerEndpoint.connect(pointerDetectorFilter);
				pointerDetectorFilter.connect(httpEP);
			}

			else {
				// Player without filter
				playerEndpoint.connect(httpEP);
			}

			session.start(httpEP);
			session.setAttribute("player", playerEndpoint);
		}
	}
}
