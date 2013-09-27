package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.WebRtcEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.WEB_RTP_END_POINT_TYPE)
public class WebRtcEndPointImpl extends SdpEndPointImpl implements
		WebRtcEndPoint {

	WebRtcEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

}
