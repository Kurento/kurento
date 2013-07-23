package com.kurento.kms.media;

import java.io.IOException;

public class PlayerEndPoint extends UriEndPoint {

	private static final long serialVersionUID = 1L;

	PlayerEndPoint(com.kurento.kms.api.MediaObject mediaPlayer) {
		super(mediaPlayer);
	}

	/* SYNC */

	public void play() throws IOException {
		start();
	}

	/* ASYNC */

	public void play(Continuation<Void> cont) throws IOException {
		start(cont);
	}
}
