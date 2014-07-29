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

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;

/**
 * HTTP Player with FaceOverlay filter.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.2
 */
@HttpPlayerService(path = "/playerWithFilter")
public class MyPlayerWithFilter extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession contentSession)
			throws Exception {
		// Media Pipeline
		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);

		// Media Elements: Player Endpoint, Filter, HTTP Endpoint
		PlayerEndpoint playerEndpoint = mp.newPlayerEndpoint(
				"http://files.kurento.org/video/fiwarecut.webm").build();
		contentSession.setAttribute("player", playerEndpoint);

		final FaceOverlayFilter filter = mp.newFaceOverlayFilter().build();

		filter.setOverlayedImage(
				"http://files.kurento.org/imgs/mario-wings.png", -0.35F, -1.2F,
				1.6F, 1.6F);

		HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();

		// Connections
		filter.connect(httpEndpoint);
		playerEndpoint.connect(filter);

		// Start content session
		contentSession.start(httpEndpoint);
	}

	@Override
	public void onContentStarted(HttpPlayerSession contentSession) {
		PlayerEndpoint playerEndpoint = (PlayerEndpoint) contentSession
				.getAttribute("player");
		playerEndpoint.play();
	}
}
