package com.kurento.kms.media;

import java.io.IOException;

public class RecorderEndPoint extends UriEndPoint {

	private static final long serialVersionUID = 1L;

	RecorderEndPoint(com.kurento.kms.api.MediaObject mediaRecorder) {
		super(mediaRecorder);
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
