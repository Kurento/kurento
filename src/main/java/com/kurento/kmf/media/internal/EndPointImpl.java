package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.EndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public abstract class EndPointImpl extends MediaElementImpl implements EndPoint {

	public EndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}
}
