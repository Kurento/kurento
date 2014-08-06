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
package org.kurento.kmf.demo.group;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import org.kurento.kmf.jsonrpcconnector.Session;
import org.kurento.kmf.media.MediaPipeline;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
public class Room {
	private final Logger log = LoggerFactory.getLogger(Room.class);

	private final ConcurrentMap<String, Participant> participants = new ConcurrentHashMap<>();
	private final MediaPipeline pipeline;
	private final String roomName;

	/**
	 * @return the roomName
	 */
	public String getRoomName() {
		return roomName;
	}

	public Room(String roomName, MediaPipeline pipeline) {
		this.roomName = roomName;
		this.pipeline = pipeline;
		log.info("ROOM {} has been created", roomName);
	}

	@PreDestroy
	private void shutdown() {
		for (Participant participant : participants.values()) {
			try {
				participant.close();
			} catch (IOException e) {
				// nothing to do here
			}
		}
	}

	public Participant join(String name, Session session) throws IOException {
		log.info("ROOM {}: adding participant {}", roomName, name);
		Participant participant = new Participant(name, session, this.pipeline);
		joinRoom(participant);
		participants.put(name, participant);
		return participant;
	}

	/**
	 * @param participant
	 * @throws IOException
	 */
	private Collection<String> joinRoom(Participant newParticipant)
			throws IOException {
		JsonObject newParticipantAnnouncement = new JsonObject();
		newParticipantAnnouncement
				.addProperty("name", newParticipant.getName());

		List<String> participantsList = newArrayListWithExpectedSize(participants
				.values().size());
		log.debug(
				"ROOM {}: notifying other participants of new participant {}",
				roomName, newParticipant.getName());
		for (Participant participant : participants.values()) {
			try {
				participant.getSession().sendNotification(
						"newParticipantArrived", newParticipantAnnouncement);
			} catch (IOException e) {
				log.debug("ROOM {}: participant {} could not be notified",
						roomName, participant.getName(), e);
			}
			participantsList.add(participant.getName());
		}

		return participantsList;
	}

	public void removeParticipant(String name) throws IOException {

		String removedParticipantName;

		try (Participant removedParticipant = participants.remove(name)) {
			removedParticipantName = removedParticipant.getName();
		}

		log.debug("ROOM {}: notifying all users that {} is leaving the room",
				this.roomName, name);

		List<String> unnotifiedParticipants = newArrayListWithExpectedSize(participants
				.values().size());
		JsonObject participantLeftNotification = new JsonObject();
		participantLeftNotification.addProperty("name", removedParticipantName);
		for (Participant participant : participants.values()) {
			try {
				participant.cancelVideoFrom(removedParticipantName);
				participant.getSession().sendNotification("participantLeft",
						participantLeftNotification);
			} catch (IOException e) {
				unnotifiedParticipants.add(participant.getName());
			}
		}

		if (!unnotifiedParticipants.isEmpty()) {
			// TODO send the list to the client? Doesn't seem useful
			log.debug(
					"ROOM {}: The users {} could not be notified that {} left the room",
					this.roomName, unnotifiedParticipants, name);
		}

	}

	/**
	 * @return a collection with all the participants in the room
	 */
	public Collection<Participant> getParticipants() {
		return participants.values();
	}

	/**
	 * @param name
	 * @return the participant from this session
	 */
	public Participant getParticipant(String name) {
		return participants.get(name);
	}

}
