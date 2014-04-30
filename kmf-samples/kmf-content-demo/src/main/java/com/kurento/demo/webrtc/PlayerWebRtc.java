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

/**
 * HTTP Player of previously recorded WebRTC content; tunnel strategy
 * (redirect=false, by default); not using JSON-RPC control protocol
 * (useControlProtocol=true).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 3.0.7
 */
@HttpPlayerService(path = "/playerWebRtc/*", useControlProtocol = true)
public class PlayerWebRtc extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		// If the contentId is present, then it determines the name of the file
		// to be played, stored in the Media Server at /tmp folder
		final String contentId = session.getContentId();
		final String url = contentId != null ? "file:///tmp/" + contentId
				: WebRtcRecorder.TARGET;
		session.start(url);
	}

}
