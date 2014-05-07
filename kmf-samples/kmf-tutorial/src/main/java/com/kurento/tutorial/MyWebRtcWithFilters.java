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
package com.kurento.tutorial;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PointerDetectorFilter;
import com.kurento.kmf.media.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;

/**
 * WebRTC with PointerDetector and JackVader filters.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.2
 */
@WebRtcContentService(path = "/webRtcWithFilters")
public class MyWebRtcWithFilters extends WebRtcContentHandler {

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		// Media Pipeline
		final MediaPipeline mp = contentSession.getMediaPipelineFactory()
				.create();
		contentSession.releaseOnTerminate(mp);

		// Media Elements: WebRTC Endpoint, Filter
		final WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		final PointerDetectorFilter pointerDetectorFilter = mp
				.newPointerDetectorFilter().build();
		final FaceOverlayFilter faceOverlayFilter = mp.newFaceOverlayFilter()
				.build();
		PointerDetectorWindowMediaParam start = new PointerDetectorWindowMediaParam(
				"start", 100, 100, 280, 380);
		start.setImage("http://ci.kurento.com/imgs/start.png");
		pointerDetectorFilter.addWindow(start);
		pointerDetectorFilter
				.addWindowInListener(new MediaEventListener<WindowInEvent>() {
					public void onEvent(WindowInEvent event) {
						// Set overlay image
						faceOverlayFilter.setOverlayedImage(
								"http://ci.kurento.com/imgs/mario-wings.png",
								-0.35F, -1.2F, 1.6F, 1.6F);
					}
				});

		// Connections
		webRtcEndpoint.connect(pointerDetectorFilter);
		pointerDetectorFilter.connect(faceOverlayFilter);
		faceOverlayFilter.connect(webRtcEndpoint);

		// Start content session
		contentSession.start(webRtcEndpoint);
	}

}
