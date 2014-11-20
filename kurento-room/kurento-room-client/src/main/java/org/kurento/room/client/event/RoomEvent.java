package org.kurento.room.client.event;

import org.kurento.room.client.Room;

public class RoomEvent extends Event {

	public RoomEvent(Room source) {
		super(source);
	}

	@Override
	public Object getSource() {
		return (Room) source;
	}

}
