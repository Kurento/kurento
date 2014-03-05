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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * WebRtc Handler for OneToMany rooms
 * 
 * @author Miguel París Díaz (mparisdiaz@gmail.com)
 */
@WebRtcContentService(path = "/selectable/*")
public class SelectableRoomHandler extends WebRtcContentHandler {

	private static final Logger log = LoggerFactory
			.getLogger(SelectableRoomHandler.class);

	public static final String ATTR_NAME = "name";

	/* Commands */
	public static final String COMMAND_GET_PARTICIPANTS = "getParticipants";
	public static final String COMMAND_SELECT = "selectParticipant";
	public static final String COMMAND_CONNECT = "connectParticipant";

	/* Events */
	public static final String EVENT_ON_JOINED = "onJoined";
	public static final String EVENT_ON_UNJOINED = "onUnjoined";

	private MediaPipeline mp;
	public final Map<WebRtcContentSession, Participant> sessions = new ConcurrentHashMap<WebRtcContentSession, Participant>();
	public final Map<String, Participant> participants = new ConcurrentHashMap<String, Participant>();
	public final Set<ParticipantPojo> participantsPojo = new CopyOnWriteArraySet<ParticipantPojo>();

	private static final Gson gson = new Gson();

	/* Commands */
	private Set<ParticipantPojo> getParticipants() {
		return participantsPojo;
	}

	private boolean selectParticipant(WebRtcContentSession session,
			String partId) {
		Participant partSelected = participants.get(partId);
		if (partSelected == null) {
			log.error("Participant {} does not exist", partSelected);
			return false;
		}

		partSelected.endpoint.connect(sessions.get(session).endpoint);

		return true;
	}

	private boolean connectParticipant(String origId, String destId) {
		Participant orig = participants.get(origId);
		if (orig == null) {
			log.error("Participant " + origId + " does not exist");
			return false;
		}

		Participant dest = participants.get(destId);
		if (dest == null) {
			log.error("Participant " + destId + " does not exist");
			return false;
		}

		orig.endpoint.connect(dest.endpoint);

		return true;
	}

	/* Events */
	private void notifyJoined(Participant participant) {
		String json = participant.toJson();
		log.debug("Participant joined: {}", json);

		for (WebRtcContentSession session : sessions.keySet()) {
			session.publishEvent(new ContentEvent(EVENT_ON_JOINED, json));
		}
	}

	private void notifyUnjoined(Participant participant) {
		String json = participant.toJson();
		log.debug("Participant unjoined: {}", json);

		for (WebRtcContentSession session : sessions.keySet()) {
			session.publishEvent(new ContentEvent(EVENT_ON_UNJOINED, json));
		}
	}

	@Override
	public void onContentRequest(WebRtcContentSession session) throws Exception {
		synchronized (this) {
			if (mp == null) {
				mp = session.getMediaPipelineFactory().create();
			}
		}

		String name = session.getHttpServletRequest().getParameter(ATTR_NAME);
		if (name == null || name.isEmpty()) {
			name = "<no name>";
		}

		WebRtcEndpoint endpoint = mp.newWebRtcEndpoint().build();
		Participant participant = new Participant(name, endpoint);

		participants.put(participant.getId(), participant);
		participantsPojo.add(participant.pojo);
		sessions.put(session, participant);

		participant.endpoint.connect(participant.endpoint);
		session.start(participant.endpoint);
		notifyJoined(participant);
	}

	@Override
	public ContentCommandResult onContentCommand(WebRtcContentSession session,
			ContentCommand command) throws Exception {
		String cmdType = command.getType();
		String cmdData = command.getData();
		log.debug("onContentCommand: ({}, {})", cmdType, cmdData);

		if (COMMAND_GET_PARTICIPANTS.equalsIgnoreCase(cmdType)) {
			String json = gson.toJson(getParticipants());
			return new ContentCommandResult(json);
		} else if (COMMAND_SELECT.equalsIgnoreCase(cmdType)) {
			return new ContentCommandResult(Boolean.toString(selectParticipant(
					session, cmdData)));
		} else if (COMMAND_CONNECT.equalsIgnoreCase(cmdType)) {
			Type listType = new TypeToken<List<String>>() {
			}.getType();
			List<String> idList = gson.fromJson(cmdData, listType);

			if (idList.size() != 2) {
				return new ContentCommandResult(Boolean.toString(false));
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
		Participant participant = sessions.get(session);
		sessions.remove(session);
		participants.remove(participant.getId());
		participantsPojo.remove(participant.pojo);
		notifyUnjoined(participant);

		super.onSessionTerminated(session, code, reason);
	}

	private static class ParticipantPojo {

		private String id;
		private String name;

		private ParticipantPojo(String name) {
			id = generateId();
			this.name = name;
		}

		private static int globalId = 0;

		private static synchronized String generateId() {
			return Integer.toString(globalId++);
		}
	}

	private static class Participant {

		private ParticipantPojo pojo;
		private static final Gson gson = new Gson();

		private WebRtcEndpoint endpoint;

		private String getId() {
			return pojo.id;
		}

		private String toJson() {
			return gson.toJson(pojo);
		}

		private Participant(String name, WebRtcEndpoint webRtcEndpoint) {
			pojo = new ParticipantPojo(name);
			endpoint = webRtcEndpoint;
		}

	}

}
