package com.kurento.kmf.media;

import java.io.IOException;

import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.UriEndPointType;

public class RecorderEndPoint extends UriEndPoint {

	private static final long serialVersionUID = 1L;

	static final UriEndPointType uriEndPointType = UriEndPointType.RECORDER_END_POINT;

	RecorderEndPoint(MediaObjectId recorderEndPointId) {
		super(recorderEndPointId);
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
