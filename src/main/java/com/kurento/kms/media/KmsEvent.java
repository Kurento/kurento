package com.kurento.kms.media;

public class KmsEvent {

	private final Event type;
	private final MediaObject source;

	KmsEvent(Event type, MediaObject source) {
		this.type = type;
		this.source = source;
	}

	// TODO: Possible definition in Thrift
	enum Event {
		DEFAULT,
	}

	public Event getEvent() {
		return type;
	}

	public MediaObject getSource() {
		return source;
	}
}
