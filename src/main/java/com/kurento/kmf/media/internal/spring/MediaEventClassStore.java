package com.kurento.kmf.media.internal.spring;

import com.kurento.kmf.media.internal.ProvidesMediaEvent;

class MediaEventClassStore extends
		AbstractAnnotationClassStore<ProvidesMediaEvent> {

	MediaEventClassStore() {
		super(ProvidesMediaEvent.class);
	}

	@Override
	protected String getTypeFromAnnotation(Class<?> clazz) {
		ProvidesMediaEvent annotation = clazz
				.getAnnotation(ProvidesMediaEvent.class);
		return annotation.type();
	}

}