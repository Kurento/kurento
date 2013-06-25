package com.kurento.kms.media;

import org.apache.commons.lang.NotImplementedException;

import com.kurento.kms.api.MediaObject;

/**
 * Represents a http address where a single get or post can be done
 * 
 * @author jcaden
 * 
 */
public class HttpEndPoint extends EndPoint {

	private static final long serialVersionUID = 1L;

	HttpEndPoint(MediaObject mediaStream) {
		super(mediaStream);
	}

	/* SYNC */

	public String getUri() {
		// TODO: Implement this method
		throw new NotImplementedException();
	}

	/* ASYNC */

	public void getUri(Continuation<String> cont) {
		// TODO: Implement this method
		cont.onError(new NotImplementedException());
	}
}
