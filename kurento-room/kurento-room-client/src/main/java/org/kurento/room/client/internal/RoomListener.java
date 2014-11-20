package org.kurento.room.client.internal;

public interface RoomListener {

	void streamAddedEvent(StreamInfo streamInfo);

	void streamRemovedEvent(String streamId);

}
