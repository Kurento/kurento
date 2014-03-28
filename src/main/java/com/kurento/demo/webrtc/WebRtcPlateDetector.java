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
package com.kurento.demo.webrtc;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;

/**
 * WebRtc Handler in loopback with plate dectetor filter.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.1.2
 */
@WebRtcContentService(path = "/webRtcPlateDetector")
public class WebRtcPlateDetector extends WebRtcContentHandler {

	@Override
	public void onContentRequest(final WebRtcContentSession session)
			throws Exception {
		MediaPipeline mp = session.getMediaPipelineFactory().create();
		session.releaseOnTerminate(mp);

		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		PlateDetectorFilter plateDetectorFilter = mp.newPlateDetectorFilter()
				.build();
		session.setAttribute("plateValue", "");
		plateDetectorFilter
				.addPlateDetectedListener(new MediaEventListener<PlateDetectedEvent>() {
					@Override
					public void onEvent(PlateDetectedEvent event) {
						getLogger().info("Plate detected {}", event);
						if (session.getAttribute("plateValue").toString()
								.equals(event.getPlate())) {
							return;
						}
						session.setAttribute("plateValue", event.getPlate());
						session.publishEvent(new ContentEvent(event.getType(),
								event.getPlate()));
					}
				});

		webRtcEndpoint.connect(plateDetectorFilter);
		plateDetectorFilter.connect(webRtcEndpoint);

		session.start(webRtcEndpoint);
	}

}
