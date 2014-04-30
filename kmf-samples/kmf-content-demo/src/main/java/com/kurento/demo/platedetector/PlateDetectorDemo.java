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
package com.kurento.demo.platedetector;

import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;

/**
 * Plate Detector demo.
 * 
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @since 4.2.3
 */
@WebRtcContentService(path = "/plateDetectorDemo/*")
public class PlateDetectorDemo extends WebRtcContentHandler {

	public MediaPipeline mediaPipeline;
	public WebRtcEndpoint webRtcEndpoint;
	public PlateDetectorFilter plateDetector;

	@Override
	public void onContentRequest(final WebRtcContentSession contentSession)
			throws Exception {
		MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
		mediaPipeline = mpf.create();
		contentSession.releaseOnTerminate(mediaPipeline);

		webRtcEndpoint = mediaPipeline.newWebRtcEndpoint().build();
		plateDetector = mediaPipeline.newPlateDetectorFilter().build();

		webRtcEndpoint.connect(plateDetector);
		plateDetector.connect(webRtcEndpoint);

		contentSession.start(webRtcEndpoint);

		contentSession.setAttribute("eventValue", "");
		plateDetector
				.addPlateDetectedListener(new MediaEventListener<PlateDetectedEvent>() {
					@Override
					public void onEvent(PlateDetectedEvent event) {
						if (contentSession.getAttribute("eventValue")
								.toString().equals(event.getPlate())) {
							return;
						}
						contentSession.setAttribute("eventValue",
								event.getPlate());
						contentSession.publishEvent(new ContentEvent(event
								.getType(), event.getPlate()));
					}
				});
	}

	@Override
	public ContentCommandResult onContentCommand(
			WebRtcContentSession contentSession, ContentCommand contentCommand)
			throws Exception {
		if ("changeWidth".equalsIgnoreCase(contentCommand.getType())) {
			plateDetector.setPlateWidthPercentage(Float
					.parseFloat(contentCommand.getData()));
		}
		return new ContentCommandResult(contentCommand.getData());
	}
}
