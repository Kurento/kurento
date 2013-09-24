package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = WebRtcEndPoint.TYPE)
public class WebRtcEndPoint extends SdpEndPoint {

	public static final String TYPE = "WebRtcEndPoint";

	WebRtcEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

}
