package org.kurento.room.client.event;

import org.kurento.room.client.Stream;

public class StreamEvent extends Event {

	public StreamEvent(Stream source) {
		super(source);
	}

	@Override
	public Object getSource() {
		return (Stream) source;
	}

}
