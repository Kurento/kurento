package com.kurento.kmf.media;

import com.kurento.kms.api.MediaObject;

public abstract class Filter extends MediaElement {

	private static final long serialVersionUID = 1L;

	Filter(MediaObject filter) {
		super(filter);
	}
}
