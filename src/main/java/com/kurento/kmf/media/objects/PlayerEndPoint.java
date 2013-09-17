package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "PlayerEndPoint")
public class PlayerEndPoint extends UriEndPoint {

	PlayerEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	public void play() {
	}

	/* ASYNC */
	public void play(Continuation<Void> cont) {

	}

}
