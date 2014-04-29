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
 * WebRTC video conference (one to many).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.2
 */
@WebRtcContentService(path = "/webRtcOneToMany")
public class MyWebRtcOneToMany extends WebRtcContentHandler {

	private WebRtcEndpoint firstWebRtcEndpoint;

	private String sessionId;

	@Override
	public synchronized void onContentRequest(
			WebRtcContentSession contentSession) throws Exception {
		if (firstWebRtcEndpoint == null) {
			// Media Pipeline creation
			MediaPipeline mp = contentSession.getMediaPipelineFactory()
					.create();
			contentSession.releaseOnTerminate(mp);

			// First WebRTC enpoint in loopback
			firstWebRtcEndpoint = mp.newWebRtcEndpoint().build();
			sessionId = contentSession.getSessionId();
			contentSession.releaseOnTerminate(firstWebRtcEndpoint);
			firstWebRtcEndpoint.connect(firstWebRtcEndpoint);

			contentSession.start(firstWebRtcEndpoint);
		} else {
			// Media Pipeline reusing
			MediaPipeline mp = firstWebRtcEndpoint.getMediaPipeline();

			// Next WebRTC endpoints connected to the first one
			WebRtcEndpoint newWebRtcEndpoint = mp.newWebRtcEndpoint().build();
			contentSession.releaseOnTerminate(newWebRtcEndpoint);
			newWebRtcEndpoint.connect(firstWebRtcEndpoint);
			firstWebRtcEndpoint.connect(newWebRtcEndpoint);

			contentSession.start(newWebRtcEndpoint);
		}
	}

	@Override
	public void onSessionTerminated(WebRtcContentSession contentSession,
			int code, String reason) throws Exception {
		if (contentSession.getSessionId().equals(sessionId)) {
			getLogger().info("Terminating first WebRTC session");
			firstWebRtcEndpoint = null;
		}
		super.onSessionTerminated(contentSession, code, reason);
	}
}
