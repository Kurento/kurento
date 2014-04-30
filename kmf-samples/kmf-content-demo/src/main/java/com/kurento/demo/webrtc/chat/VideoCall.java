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
package com.kurento.demo.webrtc.chat;

import java.util.HashMap;
import java.util.Map;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.content.jsonrpc.Constraints;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Handler implementing a video conference chat room using WebRtcEnpoints.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 3.0.7
 */
@WebRtcContentService(path = "/videocall/*")
public class VideoCall extends WebRtcContentHandler {

	protected Map<String, Connection> connections;

	protected MediaPipeline mp;

	private static int ROOM_SIZE = 5;

	@Override
	public synchronized void onContentRequest(
			WebRtcContentSession contentSession) throws Exception {

		if (connections == null) {
			getLogger().debug("Creating connections");
			connections = new HashMap<String, Connection>();
			mp = contentSession.getMediaPipelineFactory().create();
			contentSession.releaseOnTerminate(mp);
		}

		final String nickName = contentSession.getContentId();
		Connection connection = null;
		if (!connections.containsKey(nickName)) {
			getLogger().debug("First request of " + nickName);

			// We cannot make more connections than the room size
			if (connections.size() == ROOM_SIZE) {
				roomFull(contentSession);
				return;
			}

			// The first time, connections must be setup
			connection = new Connection(ROOM_SIZE);
			connections.put(nickName, connection);

		} else {
			connection = connections.get(nickName);

			getLogger().debug("Further requests of " + nickName);
			if (connection.allElementsPresent()) {
				if (connections.size() == ROOM_SIZE) {
					// We cannot make more connections than the room size
					roomFull(contentSession);
				} else {
					// If connection is full it means that another session is
					// trying to connect using the same nick name, so we
					// terminate the session and return an error
					contentSession
							.terminate(403,
									"The nick name is in use. Please change it and try again.");
				}
				return;
			}
		}

		// Build WebRtcEndpoint
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		contentSession.releaseOnTerminate(webRtcEndpoint);

		// Check if it is a transmitter or a receiver
		Constraints videoConstraints = contentSession.getVideoConstraints();

		if (videoConstraints.name().equalsIgnoreCase("SENDONLY")) {
			// it means the request is from the transmitter
			connection.setTransmitter(webRtcEndpoint, nickName);
			contentSession.publishEvent(new ContentEvent("nickname", nickName));
		} else {
			connection.addReceiver(webRtcEndpoint, contentSession);
		}

		// If at this moment the connection is full, then it can be established
		// the connections with other elements
		if (connection.allElementsPresent()) {
			for (String s : connections.keySet()) {
				if (!s.equals(nickName)) {
					connection.connectTransmitter(connections.get(s)
							.getReceivers());
					connection.connectReceivers(connections.get(s)
							.getTransmitter());
				}
			}
		}

		// Finally the session is started
		contentSession.start(webRtcEndpoint);
	}

	private void roomFull(WebRtcContentSession contentSession) {
		contentSession.terminate(403, "The room is full. Please try later.");
	}

	@Override
	public void onSessionTerminated(WebRtcContentSession contentSession,
			int code, String reason) throws Exception {
		final String nickName = contentSession.getContentId();
		if (connections != null && connections.containsKey(nickName)) {
			getLogger().debug("Removing {} of the video chat room", nickName);
			connections.remove(nickName);
			if (connections.isEmpty()) {
				getLogger().debug("Destroying {} video chat room", connections);
				connections = null;
			}
		}
		super.onSessionTerminated(contentSession, code, reason);
	}
}
