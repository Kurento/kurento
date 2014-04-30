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
package com.kurento.demo.webrtc;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * WebRTC handler with FaceOverlayFilter, in loopback.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@WebRtcContentService(path = "/webRtcFaceOverlayLoopback")
public class WebRtcFaceOverlayLoopback extends WebRtcContentHandler {

	@Autowired
	private MediaApiConfiguration config;

	public static FaceOverlayFilter filter;

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {

		String imageUrl = contentSession.getHttpServletRequest().getScheme()
				+ "://" + config.getHandlerAddress() + ":"
				+ contentSession.getHttpServletRequest().getServerPort()
				+ contentSession.getHttpServletRequest().getContextPath()
				+ "/img/masks/mario-wings.png";

		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);
		filter = mp.newFaceOverlayFilter().build();
		filter.setOverlayedImage(imageUrl, -0.35F, -1.2F, 1.6F, 1.6F);
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		webRtcEndpoint.connect(filter);
		filter.connect(webRtcEndpoint);
		contentSession.start(webRtcEndpoint);
	}

	@Override
	public void onSessionTerminated(WebRtcContentSession contentSession,
			int code, String reason) throws Exception {
		super.onSessionTerminated(contentSession, code, reason);
		filter = null;
	}
}
