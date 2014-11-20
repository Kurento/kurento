package org.kurento.room.server.app;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ManagerRoomClient {

	private ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

	public Room createRoom(String name) {
		String roomId = UUID.randomUUID().toString();
		Room room = new Room(name, roomId);
		rooms.put(roomId, room);
		return room;
	}

	public Collection<Room> getRooms() {
		return Collections.unmodifiableCollection(rooms.values());
	}

	public Room getRoom(String roomId) {
		return rooms.get(roomId);
	}

	public Room deleteRoom(String roomId) {
		return rooms.remove(roomId);
	}

	public String createToken(String roomId, String name, String role) {
		return rooms.get(roomId).addUser(name, role);
	}
}
