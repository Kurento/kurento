package org.kurento.room.client.event;

import org.kurento.room.client.Stream;

public class StreamAttributeUpdateEvent extends StreamEvent {

	public StreamAttributeUpdateEvent(Stream source) {
		super(source);
	}

}
