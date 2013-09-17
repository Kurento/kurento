package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "RtpEndPoint")
public class RtpEndPoint extends SdpEndPoint {

	RtpEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

}
