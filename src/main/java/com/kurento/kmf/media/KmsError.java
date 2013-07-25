package com.kurento.kmf.media;

public class KmsError {

	private final Error type;
	private final MediaObject source;

	KmsError(Error type, MediaObject source) {
		this.type = type;
		this.source = source;
	}

	// TODO: Possible definition in Thrift
	enum Error {
		NO_ERROR,
	}

	public Error getError() {
		return type;
	}

	public MediaObject getSource() {
		return source;
	}
}
