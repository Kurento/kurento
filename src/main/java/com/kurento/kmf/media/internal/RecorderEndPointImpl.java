package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.RecorderEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.RECORDER_END_POINT_TYPE)
public class RecorderEndPointImpl extends UriEndPointImpl implements
		RecorderEndPoint {

	RecorderEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public void record() {
		start();
	}

	/* ASYNC */
	@Override
	public void record(final Continuation<Void> cont) {
		start(cont);
	}
}
