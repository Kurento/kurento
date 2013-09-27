package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.PLAYER_END_POINT_TYPE)
public class PlayerEndPointImpl extends UriEndPointImpl implements
		PlayerEndPoint {

	PlayerEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public void play() {
		start();
	}

	/* ASYNC */
	@Override
	public void play(final Continuation<Void> cont) {
		start(cont);
	}
}
