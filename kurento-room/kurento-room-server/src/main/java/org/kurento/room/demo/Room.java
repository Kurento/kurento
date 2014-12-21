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
package org.kurento.room.demo;

import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kurento.client.Continuation;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.commons.exception.KurentoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class Room implements Closeable {
	private final Logger log = LoggerFactory.getLogger(Room.class);

	private final ConcurrentMap<String, RoomParticipant> participants = new ConcurrentHashMap<>();
	private final String name;

	private MediaPipeline pipeline;

	private KurentoClient kurento;

	private volatile boolean closed = false;

	private ExecutorService executor = Executors.newFixedThreadPool(1);

	public Room(String roomName, KurentoClient kurento) {
		this.name = roomName;
		this.kurento = kurento;
		log.info("ROOM {} has been created", roomName);
	}

	public String getName() {
		return name;
	}

	public RoomParticipant join(String userName, WebSocketSession session) {

		checkClosed();

		if (pipeline == null) {
			log.info("ROOM {}: Creating MediaPipeline", userName);
			pipeline = kurento.createMediaPipeline();
		}

		log.info("ROOM {}: adding participant {}", userName, userName);
		final RoomParticipant participant = new RoomParticipant(userName, this,
				session, this.pipeline);

		sendParticipantNames(participant);

		final JsonObject newParticipantMsg = new JsonObject();
		newParticipantMsg.addProperty("id", "newParticipantArrived");
		newParticipantMsg.addProperty("name", participant.getName());

		log.debug(
				"ROOM {}: notifying other participants {} of new participant {}",
				name, participants.values(), participant.getName());

		for (final RoomParticipant participant1 : participants.values()) {
			participant1.sendMessage(newParticipantMsg);
		}

		participants.put(participant.getName(), participant);

		log.debug(
				"ROOM {}: Notified other participants {} of new participant {}",
				name, participants.values(), participant.getName());

		return participant;
	}

	private void checkClosed() {
		if (closed) {
			throw new KurentoException("The room '" + name + "' is closed");
		}
	}

	public void leave(RoomParticipant user) {

		checkClosed();

		log.debug("PARTICIPANT {}: Leaving room {}", user.getName(), this.name);
		this.removeParticipant(user.getName());
		user.close();
	}

	private void removeParticipant(String name) {

		checkClosed();

		participants.remove(name);

		log.debug("ROOM {}: notifying all users that {} is leaving the room",
				this.name, name);

		final JsonObject participantLeftJson = new JsonObject();
		participantLeftJson.addProperty("id", "participantLeft");
		participantLeftJson.addProperty("name", name);
		for (final RoomParticipant participant : participants.values()) {
			participant.cancelSendingVideoTo(name);
			participant.sendMessage(participantLeftJson);
		}
	}

	public void sendParticipantNames(RoomParticipant user) {

		checkClosed();

		log.debug("PARTICIPANT {}: sending a list of participants",
				user.getName());

		final JsonArray participantsArray = new JsonArray();
		for (final RoomParticipant participant : this.getParticipants()) {
			log.debug("PARTICIPANT {}: visiting participant", user.getName(),
					participant.getName());
			if (!participant.equals(user)) {
				final JsonElement participantName = new JsonPrimitive(
						participant.getName());
				participantsArray.add(participantName);
			}
		}

		final JsonObject existingParticipantsMsg = new JsonObject();
		existingParticipantsMsg.addProperty("id", "existingParticipants");
		existingParticipantsMsg.add("data", participantsArray);
		log.debug("PARTICIPANT {}: sending a list of {} participants",
				user.getName(), participantsArray.size());
		user.sendMessage(existingParticipantsMsg);
	}

	/**
	 * @return a collection with all the participants in the room
	 */
	public Collection<RoomParticipant> getParticipants() {

		checkClosed();

		return participants.values();
	}

	/**
	 * @param name
	 * @return the participant from this session
	 */
	public RoomParticipant getParticipant(String name) {

		checkClosed();

		return participants.get(name);
	}

	@Override
	public void close() {

		if (!closed) {

			executor.shutdown();

			for (final RoomParticipant user : participants.values()) {
				user.close();
			}

			participants.clear();

			if (pipeline != null) {
				pipeline.release(new Continuation<Void>() {

					@Override
					public void onSuccess(Void result) throws Exception {
						log.trace("ROOM {}: Released Pipeline", Room.this.name);
					}

					@Override
					public void onError(Throwable cause) throws Exception {
						log.warn("PARTICIPANT " + Room.this.name
								+ ": Could not release Pipeline", cause);
					}
				});
			}

			log.debug("Room {} closed", this.name);

			this.closed = true;
		} else {
			log.warn("Closing a yet closed room {}", this.name);
		}
	}

	public void execute(Runnable task) {

		checkClosed();

		if (!executor.isShutdown()) {
			try {
				executor.submit(task).get();
			} catch (InterruptedException e) {
				return;
			} catch (ExecutionException e) {
				log.warn("Exception while executing a task in room " + name, e);
			}
		}
	}

	public boolean isClosed() {
		return closed;
	}
}
