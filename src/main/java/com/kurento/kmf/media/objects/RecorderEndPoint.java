package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "RecorderEndPoint")
public class RecorderEndPoint extends UriEndPoint {

	RecorderEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	public void record() {
	}

	/* ASYNC */
	public void record(Continuation<Void> cont) {
	}

}
