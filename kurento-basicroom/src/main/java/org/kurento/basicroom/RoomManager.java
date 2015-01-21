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
package org.kurento.basicroom;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomManager {

	private final Logger log = LoggerFactory.getLogger(RoomManager.class);

	@Autowired
	private KurentoClient kurento;

	private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

	@PreDestroy
	public void close() {
		for (Room room : rooms.values()) {
			room.close();
		}
	}

	/**
	 * @param roomName
	 *            the name of the room
	 * @return the room if it was already created, or a new one if it is the
	 *         first time this room is accessed
	 */
	public Room getRoom(String roomName) {

		Room room = rooms.get(roomName);

		if (room == null) {

			room = new Room(roomName, kurento);
			Room oldRoom = rooms.putIfAbsent(roomName, room);
			if (oldRoom != null) {
				return oldRoom;
			} else {
				log.debug("Room {} not existent. Created new!", roomName);
				return room;
			}
		} else {
			return room;
		}
	}

	/**
	 * Removes a room from the list of available rooms
	 *
	 * @param room
	 * @throws IOException
	 */
	public void removeRoom(Room room) {
		this.rooms.remove(room.getName());
		room.close();
		log.info("Room {} removed and closed", room.getName());
	}

}
