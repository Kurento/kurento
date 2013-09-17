package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "HttpEndPoint")
public class HttpEndPoint extends EndPoint {

	HttpEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */

	public String getUrl() {
		return null;
	}

	/* ASYNC */

	public void getUrl(final Continuation<String> cont) {

	}

}
