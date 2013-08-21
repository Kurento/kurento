package com.kurento.kmf.media;

import com.kurento.kms.api.FilterType;
import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.MediaObjectId;

public class ZBarFilter extends Filter {

	private static final long serialVersionUID = 1L;

	static final FilterType filterType = FilterType.ZBAR_FILTER;

	ZBarFilter(MediaObjectId filterId) {
		super(filterId);
	}

	@Override
	KmsEvent deserializeEvent(MediaEvent event) {
		return new ZBarEvent(this);
	}
}
