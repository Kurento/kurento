package com.kurento.kmf.media.internal.spring;

import com.kurento.kmf.media.internal.ProvidesMediaElement;

class MediaElementClassStore extends
		AbstractAnnotationClassStore<ProvidesMediaElement> {

	MediaElementClassStore() {
		super(ProvidesMediaElement.class);
	}

	@Override
	protected String getTypeFromAnnotation(Class<?> clazz) {
		ProvidesMediaElement annotation = clazz
				.getAnnotation(ProvidesMediaElement.class);
		return annotation.type();
	}

}
