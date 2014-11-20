package org.kurento.room.client.event;

import org.kurento.room.client.Stream;

public class StreamRemovedEvent extends RoomEvent {

	protected Stream stream;

	public StreamRemovedEvent(Stream stream) {
		super(stream.getRoom());
		this.stream = stream;
	}

	public Stream getStream() {
		return stream;
	}

}
