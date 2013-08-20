package com.kurento.kmf.media;

import com.kurento.kms.api.MediaObjectId;

public abstract class EndPoint extends MediaElement {

	private static final long serialVersionUID = 1L;

	EndPoint(MediaObjectId endPointId) {
		super(endPointId);
	}
}
