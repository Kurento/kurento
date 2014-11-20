package org.kurento.room.client.event;

import org.kurento.room.client.Stream;

public class StreamDataEvent extends StreamEvent {

	private String message;

	public StreamDataEvent(Stream source, String message) {
		super(source);
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}
