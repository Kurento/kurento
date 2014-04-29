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
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * QWebRTC in loopback.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.2
 */
@WebRtcContentService(path = "/webRtcLoopback")
public class MyWebRtcLoopback extends WebRtcContentHandler {

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		// Media Pipeline
		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);

		// Media Elements: WebRTC Endpoint
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();

		// Connections
		webRtcEndpoint.connect(webRtcEndpoint);

		// Start content session
		contentSession.start(webRtcEndpoint);
	}

}
