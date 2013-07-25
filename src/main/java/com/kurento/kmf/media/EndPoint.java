package com.kurento.kmf.media;

import com.kurento.kms.api.MediaObject;

public abstract class EndPoint extends MediaElement {

	private static final long serialVersionUID = 1L;

	EndPoint(MediaObject endPoint) {
		super(endPoint);
	}
}
