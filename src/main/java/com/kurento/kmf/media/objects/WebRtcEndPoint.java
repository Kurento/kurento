package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "WebRtcEndPoint")
public class WebRtcEndPoint extends SdpEndPoint {

	WebRtcEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

}
