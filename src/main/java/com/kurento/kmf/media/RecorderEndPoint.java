package com.kurento.kmf.media;

import java.io.IOException;

public class RecorderEndPoint extends UriEndPoint {

	private static final long serialVersionUID = 1L;

	RecorderEndPoint(com.kurento.kms.api.MediaObject recorderEndPoint) {
		super(recorderEndPoint);
	}

	/* SYNC */

	public void record() throws IOException {
		start();
	}

	/* ASYNC */

	public void record(Continuation<Void> cont) throws IOException {
		start(cont);
	}

}
