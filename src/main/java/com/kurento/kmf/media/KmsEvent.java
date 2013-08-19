package com.kurento.kmf.media;

public class KmsEvent {

	private final MediaObject source;

	KmsEvent(MediaObject source) {
		this.source = source;
	}

	public MediaObject getSource() {
		return source;
	}
}
