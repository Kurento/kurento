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

import java.util.HashMap;
import java.util.Map;

import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * This handler implements a one to many video conference using WebRtcEnpoints.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.1.2
 */
@WebRtcContentService(path = "/webRtcOneToMany")
public class WebRtcOneToMany extends WebRtcContentHandler {

	private Map<String, WebRtcContentSession> sessions;

	private static final String WEBRTCKEY = "webRtcEndpoint";

	@Override
	public synchronized void onContentRequest(WebRtcContentSession session)
			throws Exception {
		getLogger().info("#### {}", session.getSessionId());
		session.start((WebRtcEndpoint) session.getAttribute(WEBRTCKEY));
	}

	@Override
	public ContentCommandResult onContentCommand(WebRtcContentSession session,
			ContentCommand command) throws Exception {
		String cmdType = command.getType();
		String cmdData = command.getData();
		getLogger().info("onContentCommand: ({}, {})", cmdType, cmdData);

		String result = "";
		if ("register".equalsIgnoreCase(cmdType)) {
			if (sessions == null) {
				sessions = new HashMap<String, WebRtcContentSession>();
				MediaPipeline mp = session.getMediaPipelineFactory().create();
				session.releaseOnTerminate(mp);
				WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
				session.releaseOnTerminate(webRtcEndpoint);
				session.setAttribute(WEBRTCKEY, webRtcEndpoint);
				getLogger().info("**** {}", session.getSessionId());

				sessions.put(session.getSessionId(), session);
				result = "Waiting for another user in the room";

			} else {
				WebRtcEndpoint firstWebRtcEndpoint = (WebRtcEndpoint) sessions
						.values().iterator().next().getAttribute(WEBRTCKEY);
				MediaPipeline mp = firstWebRtcEndpoint.getMediaPipeline();
				WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
				session.releaseOnTerminate(webRtcEndpoint);
				webRtcEndpoint.connect(firstWebRtcEndpoint);
				session.setAttribute(WEBRTCKEY, webRtcEndpoint);

				getLogger().info("++++ {}", session.getSessionId());

				firstWebRtcEndpoint.connect(webRtcEndpoint);
				webRtcEndpoint.connect(firstWebRtcEndpoint);

				sessions.put(session.getSessionId(), session);
				result = "Connecting users";

				for (WebRtcContentSession s : sessions.values()) {
					getLogger().info("---- publishEvent {} ", s.getSessionId());
					s.publishEvent(new ContentEvent("startConn", "startConn"));
				}
			}
			return new ContentCommandResult(result);
		}
		return super.onContentCommand(session, command);
	}

	@Override
	public void onSessionTerminated(WebRtcContentSession session, int code,
			String reason) throws Exception {
		sessions.remove(session.getSessionId());
		if (sessions.isEmpty()) {
			getLogger().info("Terminating all WebRTC sessions");
			sessions = null;
		}
		super.onSessionTerminated(session, code, reason);
	}
}
