package com.kurento.kmf.media;

import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.SdpEndPointType;

public class RtpEndPoint extends SdpEndPoint {

	private static final long serialVersionUID = 1L;

	static final SdpEndPointType sdpEndPointType = SdpEndPointType.RTP_END_POINT;

	public RtpEndPoint(MediaObjectId rtpEndPointId) {
		super(rtpEndPointId);
	}

}
