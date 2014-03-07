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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
	public final Map<WebRtcContentSession, Participant> sessions = new ConcurrentHashMap();
	public final Map<String, Participant> participants = new ConcurrentHashMap();
	private static final Gson gson = new GsonBuilder()
			.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT)
			.create();

	private boolean selectParticipant(WebRtcContentSession session,
			String partId) {
		Participant partSelected = participants.get(partId);
		if (partSelected == null) {
			log.error("Participant {} does not exist", partId);
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
		String json = gson.toJson(participant);
		log.debug("Participant joined: {}", json);

		for (WebRtcContentSession session : sessions.keySet()) {
			session.publishEvent(new ContentEvent(EVENT_ON_JOINED, json));
		}
	}

	private void notifyUnjoined(Participant participant) {
		String json = gson.toJson(participant);
		log.debug("Participant unjoined: {}", json);

		for (WebRtcContentSession session : sessions.keySet()) {
			session.publishEvent(new ContentEvent(EVENT_ON_UNJOINED, json));
		}
	}

	@Override
	public void onContentRequest(WebRtcContentSession session) throws Exception {
		if (mp == null) {
			synchronized (this) {
				if (mp == null) {
					mp = session.getMediaPipelineFactory().create();
				}
			}
		}

		String name = session.getHttpServletRequest().getParameter(ATTR_NAME);
		if (name == null || name.isEmpty()) {
			name = "<no name>";
		}

		WebRtcEndpoint endpoint = mp.newWebRtcEndpoint().build();
		Participant participant = new Participant(name, endpoint);

		participants.put(participant.getId(), participant);
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
		Participant participant = sessions.get(session);
		sessions.remove(session);
		participants.remove(participant.getId());
		notifyUnjoined(participant);

		super.onSessionTerminated(session, code, reason);
	}

	private static final AtomicInteger globalId = new AtomicInteger();

	private static class Participant {

		private String id;
		private String name;

		private final transient WebRtcEndpoint endpoint;

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the name
		 */
		public String getId() {
			return id;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @param id
		 *            the id to set
		 */
		public void setId(String id) {
			this.id = id;
		}

		private Participant(String name, WebRtcEndpoint webRtcEndpoint) {
			this.name = name;
			this.id = Integer.toString(globalId.incrementAndGet());
			this.endpoint = webRtcEndpoint;
		}

	}

}
