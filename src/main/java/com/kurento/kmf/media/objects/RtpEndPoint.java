package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = RtpEndPoint.TYPE)
public class RtpEndPoint extends SdpEndPoint {

	public static final String TYPE = "RtpEndPoint";

	RtpEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

}
