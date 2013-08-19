package com.kurento.kmf.media;

import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.SdpEndPointType;

public class WebRtcEndPoint extends SdpEndPoint {

	private static final long serialVersionUID = 1L;

	static final SdpEndPointType sdpEndPointType = SdpEndPointType.WEBRTC_END_POINT;

	public WebRtcEndPoint(MediaObjectId webRtcEndPointId) {
		super(webRtcEndPointId);
	}

}
