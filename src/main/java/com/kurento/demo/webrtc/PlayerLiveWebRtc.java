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

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpEndpoint;
import com.kurento.kmf.media.MediaElement;

/**
 * HTTP Player from a previously started WebRtcEncpoint element.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.1
 */
@HttpPlayerService(path = "/playerLiveWebRtc/*", redirect = true, useControlProtocol = true)
public class PlayerLiveWebRtc extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		String contentId = session.getContentId();
		MediaElement mediaElement = null;
		HttpEndpoint httpEndpoint = session.getMediaPipelineFactory().create()
				.newHttpGetEndpoint().terminateOnEOS().build();
		if (contentId.equals("faceoverlay")) {
			mediaElement = WebRtcFaceOverlayLoopback.filter;
		} else if (contentId.equals("jackvader")) {
			mediaElement = WebRtcJackVaderLoopback.filter;
		} else if (contentId.equals("loopback")) {
			mediaElement = WebRtcLoopback.webRtcEndpoint;
		}

		if (mediaElement == null) {
			session.terminate(400, "WebRTC source is not running");
		} else {
			mediaElement.connect(httpEndpoint);
			session.start(httpEndpoint);
		}
	}
}
