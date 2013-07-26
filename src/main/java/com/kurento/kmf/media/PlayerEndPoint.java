package com.kurento.kmf.media;

import java.io.IOException;

import com.kurento.kms.api.UriEndPointType;

public class PlayerEndPoint extends UriEndPoint {

	private static final long serialVersionUID = 1L;

	static final UriEndPointType uriEndPointType = UriEndPointType.PLAYER_END_POINT;

	PlayerEndPoint(com.kurento.kms.api.MediaObject playerEndPoint) {
		super(playerEndPoint);
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
