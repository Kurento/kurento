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
package com.kurento.demo.cebit;

import static java.lang.reflect.Modifier.TRANSIENT;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kurento.demo.common.WebRTCParticipant;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * WebRtc Handler for OneToMany rooms.
 * 
 * @author Miguel París Díaz (mparisdiaz@gmail.com)
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.0.1
 */
@WebRtcContentService(path = "/selectable/*")
public class SelectableRoomHandler extends WebRtcContentHandler {

	/* Commands */
	public static final String COMMAND_GET_PARTICIPANTS = "getParticipants";
	public static final String COMMAND_SELECT = "selectParticipant";
	public static final String COMMAND_CONNECT = "connectParticipant";

	/* Events */
	public static final String EVENT_ON_JOINED = "onJoined";
	public static final String EVENT_ON_UNJOINED = "onUnjoined";

	/* Global variables */
	private MediaPipeline mp;
	private Map<String, WebRTCParticipant> participants;
	private static final Gson gson = new GsonBuilder()
			.excludeFieldsWithModifiers(TRANSIENT).create();

	@Override
	public void onContentRequest(WebRtcContentSession session) throws Exception {
		if (mp == null) {
			synchronized (this) {
				if (mp == null) {
					mp = session.getMediaPipelineFactory().create();
					participants = new ConcurrentHashMap<String, WebRTCParticipant>();
				}
			}
		}
		String name = session.getContentId();
		if (name == null || name.isEmpty()) {
			name = "<no name>";
		}
		if (existName(name)) {
			session.terminate(403, "User " + name + " is already in the room. "
					+ "Please select another name and try again.");
		} else {
			WebRtcEndpoint endpoint = mp.newWebRtcEndpoint().build();
			WebRTCParticipant participant = new WebRTCParticipant(
					session.getSessionId(), name, endpoint, session);
			participant.endpoint.connect(participant.endpoint);
			session.start(participant.endpoint);
			session.setAttribute("participant", participant);
			participants.put(participant.getId(), participant);
			notifyJoined(participant);
		}
	}

	@Override
	public ContentCommandResult onContentCommand(WebRtcContentSession session,
			ContentCommand command) throws Exception {
		String cmdType = command.getType();
		String cmdData = command.getData();
		getLogger().info("onContentCommand: ({}, {})", cmdType, cmdData);

		if (COMMAND_GET_PARTICIPANTS.equalsIgnoreCase(cmdType)) {
			String json = gson.toJson(participants.values());
			return new ContentCommandResult(json);
		} else if (COMMAND_SELECT.equalsIgnoreCase(cmdType)) {
			return new ContentCommandResult(Boolean.toString(selectParticipant(
					session, cmdData)));
		} else if (COMMAND_CONNECT.equalsIgnoreCase(cmdType)) {
			Type listType = new TypeToken<List<String>>() {
			}.getType();
			List<String> idList = gson.fromJson(cmdData, listType);
			if (idList.size() != 2) {
				return new ContentCommandResult(Boolean.FALSE.toString());
			}
			return new ContentCommandResult(
					Boolean.toString(connectParticipant(idList.get(0),
							idList.get(1))));
		}
		return super.onContentCommand(session, command);
	}

	@Override
	public synchronized void onSessionTerminated(WebRtcContentSession session,
			int code, String reason) throws Exception {
		WebRTCParticipant participant = (WebRTCParticipant) session
				.getAttribute("participant");
		participants.remove(participant.getId());
		notifyUnjoined(participant);

		if (participants.isEmpty()) {
			getLogger().info("Clearing room");
			mp = null;
			participants.clear();
		}
		super.onSessionTerminated(session, code, reason);
	}

	private boolean selectParticipant(WebRtcContentSession session,
			String partId) {
		WebRTCParticipant partSelected = participants.get(partId);
		if (partSelected == null) {
			getLogger().error("Participant {} does not exist", partId);
			return false;
		}
		partSelected.endpoint.connect(((WebRTCParticipant) session
				.getAttribute("participant")).endpoint);
		return true;
	}

	private boolean connectParticipant(String origId, String destId) {
		WebRTCParticipant orig = participants.get(origId);
		if (orig == null) {
			getLogger().error("Participant {} does not exist", origId);
			return false;
		}
		WebRTCParticipant dest = participants.get(destId);
		if (dest == null) {
			getLogger().error("Participant {} does not exist", destId);
			return false;
		}
		orig.endpoint.connect(dest.endpoint);
		return true;
	}

	private boolean existName(final String name) {
		for (WebRTCParticipant p : participants.values()) {
			if (p.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	private void notifyJoined(WebRTCParticipant participant) {
		String json = gson.toJson(participant);
		getLogger().info("Participant joined: {}", json);
		for (WebRTCParticipant p : participants.values()) {
			p.session.publishEvent(new ContentEvent(EVENT_ON_JOINED, json));
		}
	}

	private void notifyUnjoined(WebRTCParticipant participant) {
		String json = gson.toJson(participant);
		getLogger().info("Participant unjoined: {}", json);
		for (WebRTCParticipant p : participants.values()) {
			p.session.publishEvent(new ContentEvent(EVENT_ON_UNJOINED, json));
		}
	}

}
