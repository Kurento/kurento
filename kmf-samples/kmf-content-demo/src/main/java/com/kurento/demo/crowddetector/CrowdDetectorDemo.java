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
import static java.lang.reflect.Modifier.TRANSIENT;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.CrowdDetectorFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.RegionOfInterest;
import com.kurento.kmf.media.RegionOfInterestConfig;
import com.kurento.kmf.media.RelativePoint;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.events.CrowdDetectorDirectionEvent;
import com.kurento.kmf.media.events.CrowdDetectorFluidityEvent;
import com.kurento.kmf.media.events.CrowdDetectorOccupancyEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

/**
 * Crowd detector demo.
 * 
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @since 4.0.1
 */
@WebRtcContentService(path = "/crowdDetector/*")
public class CrowdDetectorDemo extends WebRtcContentHandler {

	CrowdDetectorFilter crowdDetector;
	PlayerEndpoint playerendpoint;
	MediaPipeline mediaPipeline;
	WebRtcEndpoint webRtcEndpoint;
	private static final Gson gson = new GsonBuilder()
			.excludeFieldsWithModifiers(TRANSIENT).create();

	@Override
	public void onContentRequest(final WebRtcContentSession contentSession)
			throws Exception {
		contentSession.start(webRtcEndpoint);
	}

	@Override
	public ContentCommandResult onContentCommand(
			final WebRtcContentSession contentSession,
			ContentCommand contentCommand) throws Exception {

		if ("configureFilter".equalsIgnoreCase(contentCommand.getType())) {
			if (crowdDetector == null) {
				MediaPipelineFactory mpf;
				mpf = contentSession.getMediaPipelineFactory();
				mediaPipeline = mpf.create();
				contentSession.releaseOnTerminate(mediaPipeline);
				webRtcEndpoint = mediaPipeline.newWebRtcEndpoint().build();

				// create PlayerEndpoint
				playerendpoint = mediaPipeline.newPlayerEndpoint(
						"http://files.kurento.org/video/crowd_long.avi").build();

				JsonArray readedRois = gson.fromJson(contentCommand.getData(),
						JsonArray.class);

				List<RegionOfInterest> rois = newArrayList();
				for (int j = 0; j < readedRois.size(); j++) {

					JsonObject roi = (JsonObject) readedRois.get(j);

					JsonArray coordenates = (JsonArray) roi.get("coordenates");
					// create structure to configure crowddetector
					List<RelativePoint> points = new ArrayList<RelativePoint>();
					for (int i = 0; i < coordenates.size(); i++) {
						JsonObject coordenate = (JsonObject) coordenates.get(i);

						float x = coordenate.getAsJsonPrimitive("x")
								.getAsFloat();
						float y = coordenate.getAsJsonPrimitive("y")
								.getAsFloat();

						points.add(new RelativePoint(x, y));
					}

					RegionOfInterestConfig config = new RegionOfInterestConfig();

					config.setFluidityLevelMin(roi.getAsJsonPrimitive(
							"fluidityLevelMin").getAsInt());
					config.setFluidityLevelMed(roi.getAsJsonPrimitive(
							"fluidityLevelMed").getAsInt());
					config.setFluidityLevelMax(roi.getAsJsonPrimitive(
							"fluidityLevelMax").getAsInt());
					config.setFluidityNumFramesToEvent(roi.getAsJsonPrimitive(
							"fluidityNumFramesToEvent").getAsInt());
					config.setOccupancyLevelMin(roi.getAsJsonPrimitive(
							"occupancyLevelMin").getAsInt());
					config.setOccupancyLevelMed(roi.getAsJsonPrimitive(
							"occupancyLevelMed").getAsInt());
					config.setOccupancyLevelMax(roi.getAsJsonPrimitive(
							"occupancyLevelMax").getAsInt());
					config.setOccupancyNumFramesToEvent(roi.getAsJsonPrimitive(
							"occupancyNumFramesToEvent").getAsInt());

					if (roi.getAsJsonPrimitive("sendOpticalFlowEvent")
							.getAsInt() == 0) {
						config.setSendOpticalFlowEvent(false);
					} else {
						config.setSendOpticalFlowEvent(true);
					}

					config.setOpticalFlowNumFramesToEvent(roi
							.getAsJsonPrimitive("opticalFlowNumFramesToEvents")
							.getAsInt());
					config.setOpticalFlowNumFramesToReset(roi
							.getAsJsonPrimitive("opticalFlowNumFramesToReset")
							.getAsInt());
					config.setOpticalFlowAngleOffset(roi.getAsJsonPrimitive(
							"opticalFlowAngleOffset").getAsInt());

					getLogger().info(config.toString());
					rois.add(new RegionOfInterest(points, config, roi
							.getAsJsonPrimitive("id").getAsString()));
				}

				crowdDetector = mediaPipeline.newCrowdDetectorFilter(rois)
						.build();

				// connect elements
				playerendpoint.connect(crowdDetector);
				crowdDetector.connect(webRtcEndpoint);
				playerendpoint.play();

				contentSession.publishEvent(new ContentEvent("startConn",
						"startConn"));
				// addEventListener to crowddetector
				crowdDetector
						.addCrowdDetectorDirectionListener(new MediaEventListener<CrowdDetectorDirectionEvent>() {
							@Override
							public void onEvent(
									CrowdDetectorDirectionEvent event) {
								String eventText = "Direction event detect in ROI "
										+ event.getRoiID()
										+ "with direction "
										+ event.getDirectionAngle();
								contentSession.publishEvent(new ContentEvent(
										event.getType(), eventText));
							}
						});

				crowdDetector
						.addCrowdDetectorFluidityListener(new MediaEventListener<CrowdDetectorFluidityEvent>() {
							@Override
							public void onEvent(CrowdDetectorFluidityEvent event) {
								String eventText = "Fluidity event detect in ROI "
										+ event.getRoiID()
										+ ". Fluidity level = "
										+ event.getFluidityLevel()
										+ " and fluidity percentage = "
										+ event.getFluidityPercentage();
								contentSession.publishEvent(new ContentEvent(
										event.getType(), eventText));
							}
						});

				crowdDetector
						.addCrowdDetectorOccupancyListener(new MediaEventListener<CrowdDetectorOccupancyEvent>() {
							@Override
							public void onEvent(
									CrowdDetectorOccupancyEvent event) {
								String eventText = "Occupancy event detect in ROI "
										+ event.getRoiID()
										+ ". Occupancy level = "
										+ event.getOccupancyLevel()
										+ " and occupancy percentage = "
										+ event.getOccupancyPercentage();
								contentSession.publishEvent(new ContentEvent(
										event.getType(), eventText));
							}
						});
			}
		}

		return new ContentCommandResult(contentCommand.getData());
	}

	@Override
	public synchronized void onSessionTerminated(WebRtcContentSession session,
			int code, String reason) throws Exception {
		crowdDetector = null;
		super.onSessionTerminated(session, code, reason);
	}

}
