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
package com.kurento.demo.crowddetector;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.CrowdDetectorFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.Point;
import com.kurento.kmf.media.RegionOfInterest;
import com.kurento.kmf.media.RegionOfInterestConfig;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Crowd detector demo.
 * 
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @since 4.0.1
 */
@WebRtcContentService(path = "/crowdDetector/*")
public class CrowdDetectorDemo extends WebRtcContentHandler {

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		MediaPipeline mediaPipeline;
		WebRtcEndpoint webRtcEndpoint;
		CrowdDetectorFilter crowdDetector;
		MediaPipelineFactory mpf;

		mpf = contentSession.getMediaPipelineFactory();
		mediaPipeline = mpf.create();

		List<Point> points = new ArrayList<Point>();
		points.add(new Point(0, 0));
		points.add(new Point(640, 0));
		points.add(new Point(640, 480));
		points.add(new Point(0, 480));

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

		List<RegionOfInterest> rois = newArrayList(new RegionOfInterest(points,
				config, "Roi"));

		crowdDetector = mediaPipeline.newCrowdDetectorFilter(rois).build();
		contentSession.releaseOnTerminate(mediaPipeline);

		webRtcEndpoint = mediaPipeline.newWebRtcEndpoint().build();
		webRtcEndpoint.connect(crowdDetector);
		crowdDetector.connect(webRtcEndpoint);

		contentSession.start(webRtcEndpoint);
	}

	@Override
	public void onSessionTerminated(WebRtcContentSession contentSession,
			int code, String reason) throws Exception {
		super.onSessionTerminated(contentSession, code, reason);
	}
}
