package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.RtpEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.RECORDER_END_POINT_TYPE)
public class RtpEndPointImpl extends SdpEndPointImpl implements RtpEndPoint {

	RtpEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

}
