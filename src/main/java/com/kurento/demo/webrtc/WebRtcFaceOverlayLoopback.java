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

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaPipeline;

/**
 * WebRTC handler with FaceOverlayFilter, in loopback.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@WebRtcContentService(path = "/webRtcFaceOverlayLoopback")
public class WebRtcFaceOverlayLoopback extends WebRtcContentHandler {

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		String imageUri = "/opt/img/fireman.png";
		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);
		FaceOverlayFilter filter = mp.newFaceOverlayFilter().build();
		filter.setOverlayedImage(imageUri, 0.0F, 0.6F, 1.2F, 0.8F);
		contentSession.start(filter, filter);
	}
}