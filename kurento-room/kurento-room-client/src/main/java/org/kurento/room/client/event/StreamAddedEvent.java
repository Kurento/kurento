package org.kurento.room.client.event;

import org.kurento.room.client.Stream;

public class StreamAddedEvent extends RoomEvent {

	protected Stream stream;

	public StreamAddedEvent(Stream stream) {
		super(stream.getRoom());
		this.stream = stream;
	}

	public Stream getStream() {
		return stream;
	}

}
