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

import java.util.concurrent.ConcurrentMap;

import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
@WebRtcContentService(path = "/chat/*")
public class WebRtcOneToMany extends WebRtcContentHandler {

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		contentSession.setAttribute("webRtcEndpoint", webRtcEndpoint);

		String nick = contentSession.getContentId();
		ConcurrentMap<String, WebRtcContentSession> chatRoom = ChatRoom
				.getSingleton().getSharedMap();

		getLogger().info("chatRoom size {}", chatRoom.size());

		if (chatRoom.isEmpty()) {
			contentSession.start(null, (MediaElement) null);
		} else {
			WebRtcContentSession remote = chatRoom.get(chatRoom.keySet()
					.toArray()[0]);
			contentSession.start(null, remote.getSessionEndpoint());
		}

		chatRoom.put(nick, contentSession);
	}

	@Override
	public void onContentStarted(WebRtcContentSession contentSession)
			throws Exception {
		super.onContentStarted(contentSession);
	}

	@Override
	public void onSessionTerminated(WebRtcContentSession contentSession,
			int code, String reason) throws Exception {
		super.onSessionTerminated(contentSession, code, reason);
	}

	@Override
	public ContentCommandResult onContentCommand(
			WebRtcContentSession contentSession, ContentCommand contentCommand)
			throws Exception {
		// getLogger().info("onContentCommand");
		//
		// WebRtcEndpoint webRtcEndpoint = (WebRtcEndpoint) contentSession
		// .getAttribute("webRtcEndpoint");
		//
		// ConcurrentMap<String, WebRtcEndpoint> chatRoom = ChatRoom
		// .getSingleton().getSharedMap();
		// MediaElement sink = null;
		// for (WebRtcEndpoint w : chatRoom.values()) {
		// if (!w.equals(webRtcEndpoint)) {
		// sink = w;
		// }
		// }
		// contentSession.start(webRtcEndpoint, sink);
		return super.onContentCommand(contentSession, contentCommand);
	}

	@Override
	public void onSessionError(WebRtcContentSession contentSession, int code,
			String description) throws Exception {
		super.onSessionError(contentSession, code, description);
	}

	@Override
	public void onUncaughtException(WebRtcContentSession contentSession,
			Throwable exception) throws Exception {
		super.onUncaughtException(contentSession, exception);
	}

}